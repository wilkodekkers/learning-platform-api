package com.wilkodekkers.paneng_api.practice

import com.wilkodekkers.paneng_api.chapters.ChapterRepository
import com.wilkodekkers.paneng_api.common.exceptions.ConflictException
import com.wilkodekkers.paneng_api.common.exceptions.NotFoundException
import com.wilkodekkers.paneng_api.practice.dto.PracticeSessionItemResponse
import com.wilkodekkers.paneng_api.practice.dto.PracticeSessionResponse
import com.wilkodekkers.paneng_api.practice.dto.PracticeSessionSummary
import com.wilkodekkers.paneng_api.practice.dto.SubmitAnswerRequest
import com.wilkodekkers.paneng_api.practice.dto.SubmitAnswerResponse
import com.wilkodekkers.paneng_api.vocabulary.VocabularyItemRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class StartSessionResult(
    val session: PracticeSessionResponse,
    val created: Boolean,
)

@Service
class PracticeSessionService(
    private val practiceSessionRepository: PracticeSessionRepository,
    private val practiceSessionItemRepository: PracticeSessionItemRepository,
    private val chapterRepository: ChapterRepository,
    private val vocabularyItemRepository: VocabularyItemRepository,
) {

    @Transactional
    fun start(
        bookId: Long,
        chapterId: Long,
        userId: Long
    ): StartSessionResult {
        val chapter = chapterRepository.findByIdAndBookIdAndBookUserId(
            chapterId,
            bookId,
            userId
        ) ?: throw NotFoundException(
            "Chapter with ID $chapterId not found in book with ID $bookId"
        )

        val openSession = practiceSessionRepository
            .findByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS)

        if (openSession != null && openSession.chapter.id == chapterId) {
            return StartSessionResult(toResponse(openSession), created = false)
        }

        val vocabularyItems = vocabularyItemRepository
            .findAllByChapterIdAndChapterBookUserIdOrderByIdAsc(
                chapterId,
                userId
            )

        if (vocabularyItems.isEmpty()) {
            throw ConflictException(
                "Chapter with ID $chapterId has no vocabulary items to practice"
            )
        }

        if (openSession != null) {
            openSession.status = SessionStatus.ABANDONED
            openSession.endedAt = Instant.now()
            // Flush so the open session is closed before the new one is inserted.
            practiceSessionRepository.saveAndFlush(openSession)
        }

        val session = try {
            practiceSessionRepository.saveAndFlush(
                PracticeSession(user = chapter.book.user, chapter = chapter)
            )
        } catch (ex: DataIntegrityViolationException) {
            throw ConflictException("Another practice session is already open")
        }

        val items = vocabularyItems
            .shuffled()
            .mapIndexed { index, vocabularyItem ->
                PracticeSessionItem(
                    session = session,
                    vocabularyItem = vocabularyItem,
                    position = index + 1,
                    termSnapshot = vocabularyItem.term,
                    definitionSnapshot = vocabularyItem.definition,
                    answersSnapshot = vocabularyItem.answers,
                )
            }

        return StartSessionResult(
            toResponse(session, practiceSessionItemRepository.saveAll(items)),
            created = true
        )
    }

    @Transactional(readOnly = true)
    fun findCurrent(userId: Long): PracticeSessionResponse {
        val session = practiceSessionRepository
            .findByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS)
            ?: throw NotFoundException("No open practice session")

        return toResponse(session)
    }

    @Transactional(readOnly = true)
    fun findById(sessionId: Long, userId: Long): PracticeSessionResponse {
        val session = practiceSessionRepository
            .findByIdAndUserId(sessionId, userId)
            ?: throw sessionNotFound(sessionId)

        return toResponse(session)
    }

    @Transactional
    fun submitAnswer(
        sessionId: Long,
        userId: Long,
        request: SubmitAnswerRequest
    ): SubmitAnswerResponse {
        val session = practiceSessionRepository
            .findWithLockByIdAndUserId(sessionId, userId)
            ?: throw sessionNotFound(sessionId)

        requireInProgress(session)

        val items = practiceSessionItemRepository
            .findAllBySessionIdOrderByPositionAsc(sessionId)

        val item = items.find { it.id == request.sessionItemId }
            ?: throw NotFoundException(
                "Practice session item with ID ${request.sessionItemId} " +
                    "not found in session with ID $sessionId"
            )

        if (item.answeredAt != null) {
            throw ConflictException(
                "Practice session item with ID ${item.id} is already answered"
            )
        }

        val answer = request.answer

        item.submittedAnswer = answer
        item.result = when {
            answer == null -> AnswerResult.SKIPPED
            AnswerMatcher.matches(answer, item.answersSnapshot) ->
                AnswerResult.CORRECT
            else -> AnswerResult.INCORRECT
        }
        item.answeredAt = Instant.now()
        practiceSessionItemRepository.save(item)

        val remaining = items.count { it.answeredAt == null }

        var summary: PracticeSessionSummary? = null

        if (remaining == 0) {
            session.status = SessionStatus.COMPLETED
            session.endedAt = Instant.now()
            practiceSessionRepository.save(session)
            summary = summarize(items)
        }

        return SubmitAnswerResponse(
            result = item.result!!,
            correctDefinition = item.definitionSnapshot,
            remaining = remaining,
            summary = summary,
        )
    }

    @Transactional
    fun abandon(sessionId: Long, userId: Long): PracticeSessionResponse {
        val session = practiceSessionRepository
            .findWithLockByIdAndUserId(sessionId, userId)
            ?: throw sessionNotFound(sessionId)

        requireInProgress(session)

        session.status = SessionStatus.ABANDONED
        session.endedAt = Instant.now()

        return toResponse(practiceSessionRepository.save(session))
    }

    private fun requireInProgress(session: PracticeSession) {
        if (session.status != SessionStatus.IN_PROGRESS) {
            throw ConflictException(
                "Practice session with ID ${session.id} is already ended"
            )
        }
    }

    private fun sessionNotFound(sessionId: Long) =
        NotFoundException("Practice session with ID $sessionId not found")

    private fun summarize(items: List<PracticeSessionItem>) =
        PracticeSessionSummary(
            total = items.size,
            correct = items.count { it.result == AnswerResult.CORRECT },
            incorrect = items.count { it.result == AnswerResult.INCORRECT },
            skipped = items.count { it.result == AnswerResult.SKIPPED },
        )

    private fun toResponse(
        session: PracticeSession,
        items: List<PracticeSessionItem> = practiceSessionItemRepository
            .findAllBySessionIdOrderByPositionAsc(session.id)
    ) = PracticeSessionResponse(
        id = session.id,
        bookId = session.chapter.book.id,
        chapterId = session.chapter.id,
        status = session.status,
        startedAt = session.startedAt,
        endedAt = session.endedAt,
        totalItems = items.size,
        remaining = items.count { it.answeredAt == null },
        items = items.map { it.toResponse() },
        summary = if (session.status == SessionStatus.COMPLETED) {
            summarize(items)
        } else {
            null
        },
    )

    private fun PracticeSessionItem.toResponse(): PracticeSessionItemResponse {
        val answered = answeredAt != null

        return PracticeSessionItemResponse(
            id = id,
            position = position,
            term = termSnapshot,
            answered = answered,
            result = result,
            submittedAnswer = submittedAnswer,
            correctDefinition = if (answered) definitionSnapshot else null,
        )
    }
}
