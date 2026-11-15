package com.wilkodekkers.paneng_api.practice

import com.wilkodekkers.paneng_api.books.Book
import com.wilkodekkers.paneng_api.chapters.Chapter
import com.wilkodekkers.paneng_api.chapters.ChapterRepository
import com.wilkodekkers.paneng_api.common.exceptions.ConflictException
import com.wilkodekkers.paneng_api.common.exceptions.NotFoundException
import com.wilkodekkers.paneng_api.practice.dto.SubmitAnswerRequest
import com.wilkodekkers.paneng_api.users.User
import com.wilkodekkers.paneng_api.vocabulary.VocabularyItem
import com.wilkodekkers.paneng_api.vocabulary.VocabularyItemRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.dao.DataIntegrityViolationException

class PracticeSessionServiceTests {

    private val practiceSessionRepository =
        mock(PracticeSessionRepository::class.java)

    private val practiceSessionItemRepository =
        mock(PracticeSessionItemRepository::class.java)

    private val chapterRepository = mock(ChapterRepository::class.java)

    private val vocabularyItemRepository =
        mock(VocabularyItemRepository::class.java)

    private val service = PracticeSessionService(
        practiceSessionRepository,
        practiceSessionItemRepository,
        chapterRepository,
        vocabularyItemRepository
    )

    private val user = User(
        id = 1L,
        name = "Test User",
        email = "test@example.com",
        password = "hashed-password"
    )

    private val book = Book(
        id = 1L,
        title = "Thai Basics",
        author = "John Smith",
        user = user
    )

    private val chapter = Chapter(
        id = 1L,
        book = book,
        title = "Introduction",
        position = 1
    )

    private val otherChapter = Chapter(
        id = 2L,
        book = book,
        title = "Food",
        position = 2
    )

    private fun vocabularyItem(
        id: Long,
        term: String,
        definition: String,
        answers: String = definition
    ) = VocabularyItem(
        id = id,
        chapter = chapter,
        term = term,
        definition = definition,
        answers = answers
    )

    private fun session(
        id: Long = 10L,
        chapter: Chapter = this.chapter,
        status: SessionStatus = SessionStatus.IN_PROGRESS
    ) = PracticeSession(
        id = id,
        user = user,
        chapter = chapter,
        status = status
    )

    private fun sessionItem(
        session: PracticeSession,
        id: Long,
        position: Int,
        term: String = "กิน",
        definition: String = "To eat",
        answers: String = "eat; to eat",
        result: AnswerResult? = null
    ) = PracticeSessionItem(
        id = id,
        session = session,
        vocabularyItem = null,
        position = position,
        termSnapshot = term,
        definitionSnapshot = definition,
        answersSnapshot = answers,
        submittedAnswer = if (result != null) "x" else null,
        result = result,
        answeredAt = if (result != null) java.time.Instant.now() else null
    )

    private fun stubChapterLookup() {
        `when`(chapterRepository.findByIdAndBookIdAndBookUserId(1L, 1L, 1L))
            .thenReturn(chapter)
    }

    private fun stubSaves() {
        `when`(practiceSessionRepository.saveAndFlush(any(PracticeSession::class.java)))
            .thenAnswer { it.getArgument<PracticeSession>(0) }

        `when`(practiceSessionItemRepository.saveAll(anyList<PracticeSessionItem>()))
            .thenAnswer { it.getArgument<List<PracticeSessionItem>>(0) }
    }

    // START

    @Test
    fun `start should create a session with snapshotted shuffled questions`() {
        stubChapterLookup()
        stubSaves()

        `when`(
            vocabularyItemRepository
                .findAllByChapterIdAndChapterBookUserIdOrderByIdAsc(1L, 1L)
        ).thenReturn(
            listOf(
                vocabularyItem(1L, "กิน", "To eat", "eat; to eat"),
                vocabularyItem(2L, "ดื่ม", "To drink"),
                vocabularyItem(3L, "นอน", "To sleep"),
            )
        )

        val result = service.start(1L, 1L, 1L)

        assertTrue(result.created)
        assertEquals(SessionStatus.IN_PROGRESS, result.session.status)
        assertEquals(1L, result.session.bookId)
        assertEquals(1L, result.session.chapterId)
        assertEquals(3, result.session.totalItems)
        assertEquals(3, result.session.remaining)
        assertEquals(listOf(1, 2, 3), result.session.items.map { it.position })
        assertEquals(
            setOf("กิน", "ดื่ม", "นอน"),
            result.session.items.map { it.term }.toSet()
        )
        assertNull(result.session.summary)
    }

    @Test
    fun `start should not expose definitions of unanswered questions`() {
        stubChapterLookup()
        stubSaves()

        `when`(
            vocabularyItemRepository
                .findAllByChapterIdAndChapterBookUserIdOrderByIdAsc(1L, 1L)
        ).thenReturn(listOf(vocabularyItem(1L, "กิน", "To eat")))

        val result = service.start(1L, 1L, 1L)

        val item = result.session.items.single()

        assertFalse(item.answered)
        assertNull(item.correctDefinition)
        assertNull(item.result)
    }

    @Test
    fun `start should snapshot term definition and answers`() {
        stubChapterLookup()
        stubSaves()

        `when`(
            vocabularyItemRepository
                .findAllByChapterIdAndChapterBookUserIdOrderByIdAsc(1L, 1L)
        ).thenReturn(
            listOf(vocabularyItem(1L, "กิน", "To eat", "eat; to eat"))
        )

        var saved: List<PracticeSessionItem> = emptyList()

        `when`(practiceSessionItemRepository.saveAll(anyList<PracticeSessionItem>()))
            .thenAnswer {
                saved = it.getArgument<List<PracticeSessionItem>>(0)
                saved
            }

        service.start(1L, 1L, 1L)

        assertEquals("กิน", saved.single().termSnapshot)
        assertEquals("To eat", saved.single().definitionSnapshot)
        assertEquals("eat; to eat", saved.single().answersSnapshot)
        assertEquals(1L, saved.single().vocabularyItem?.id)
    }

    @Test
    fun `start should resume the open session for the same chapter`() {
        val open = session(chapter = chapter)

        stubChapterLookup()

        `when`(practiceSessionRepository.findByUserIdAndStatus(1L, SessionStatus.IN_PROGRESS))
            .thenReturn(open)

        `when`(practiceSessionItemRepository.findAllBySessionIdOrderByPositionAsc(10L))
            .thenReturn(
                listOf(
                    sessionItem(open, 100L, 1, result = AnswerResult.CORRECT),
                    sessionItem(open, 101L, 2),
                )
            )

        val result = service.start(1L, 1L, 1L)

        assertFalse(result.created)
        assertEquals(10L, result.session.id)
        assertEquals(1, result.session.remaining)

        verify(practiceSessionRepository, never())
            .saveAndFlush(any(PracticeSession::class.java))
        verify(practiceSessionItemRepository, never())
            .saveAll(anyList<PracticeSessionItem>())
    }

    @Test
    fun `start should abandon the open session for another chapter`() {
        val open = session(id = 10L, chapter = otherChapter)

        stubChapterLookup()
        stubSaves()

        `when`(practiceSessionRepository.findByUserIdAndStatus(1L, SessionStatus.IN_PROGRESS))
            .thenReturn(open)

        `when`(
            vocabularyItemRepository
                .findAllByChapterIdAndChapterBookUserIdOrderByIdAsc(1L, 1L)
        ).thenReturn(listOf(vocabularyItem(1L, "กิน", "To eat")))

        val result = service.start(1L, 1L, 1L)

        assertTrue(result.created)
        assertEquals(SessionStatus.ABANDONED, open.status)
        assertNotNull(open.endedAt)
        verify(practiceSessionRepository).saveAndFlush(open)
    }

    @Test
    fun `start should throw ConflictException and keep the open session when chapter has no items`() {
        val open = session(id = 10L, chapter = otherChapter)

        stubChapterLookup()

        `when`(practiceSessionRepository.findByUserIdAndStatus(1L, SessionStatus.IN_PROGRESS))
            .thenReturn(open)

        `when`(
            vocabularyItemRepository
                .findAllByChapterIdAndChapterBookUserIdOrderByIdAsc(1L, 1L)
        ).thenReturn(emptyList())

        assertThrows(ConflictException::class.java) {
            service.start(1L, 1L, 1L)
        }

        assertEquals(SessionStatus.IN_PROGRESS, open.status)
        assertNull(open.endedAt)
    }

    @Test
    fun `start should throw NotFoundException when chapter does not belong to user`() {
        `when`(chapterRepository.findByIdAndBookIdAndBookUserId(1L, 1L, 2L))
            .thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            service.start(1L, 1L, 2L)
        }
    }

    @Test
    fun `start should throw ConflictException when another session is opened concurrently`() {
        stubChapterLookup()

        `when`(
            vocabularyItemRepository
                .findAllByChapterIdAndChapterBookUserIdOrderByIdAsc(1L, 1L)
        ).thenReturn(listOf(vocabularyItem(1L, "กิน", "To eat")))

        `when`(practiceSessionRepository.saveAndFlush(any(PracticeSession::class.java)))
            .thenThrow(DataIntegrityViolationException("uq_practice_sessions_one_open_per_user"))

        assertThrows(ConflictException::class.java) {
            service.start(1L, 1L, 1L)
        }
    }

    // FIND CURRENT / FIND BY ID

    @Test
    fun `findCurrent should return the open session`() {
        val open = session()

        `when`(practiceSessionRepository.findByUserIdAndStatus(1L, SessionStatus.IN_PROGRESS))
            .thenReturn(open)
        `when`(practiceSessionItemRepository.findAllBySessionIdOrderByPositionAsc(10L))
            .thenReturn(listOf(sessionItem(open, 100L, 1)))

        val result = service.findCurrent(1L)

        assertEquals(10L, result.id)
        assertEquals(SessionStatus.IN_PROGRESS, result.status)
    }

    @Test
    fun `findCurrent should throw NotFoundException when there is no open session`() {
        `when`(practiceSessionRepository.findByUserIdAndStatus(1L, SessionStatus.IN_PROGRESS))
            .thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            service.findCurrent(1L)
        }
    }

    @Test
    fun `findById should reveal definitions only for answered questions`() {
        val s = session()

        `when`(practiceSessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(s)
        `when`(practiceSessionItemRepository.findAllBySessionIdOrderByPositionAsc(10L))
            .thenReturn(
                listOf(
                    sessionItem(s, 100L, 1, result = AnswerResult.INCORRECT),
                    sessionItem(s, 101L, 2),
                )
            )

        val result = service.findById(10L, 1L)

        assertEquals("To eat", result.items[0].correctDefinition)
        assertTrue(result.items[0].answered)
        assertNull(result.items[1].correctDefinition)
        assertFalse(result.items[1].answered)
    }

    @Test
    fun `findById should include a summary for a completed session`() {
        val s = session(status = SessionStatus.COMPLETED)

        `when`(practiceSessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(s)
        `when`(practiceSessionItemRepository.findAllBySessionIdOrderByPositionAsc(10L))
            .thenReturn(
                listOf(
                    sessionItem(s, 100L, 1, result = AnswerResult.CORRECT),
                    sessionItem(s, 101L, 2, result = AnswerResult.INCORRECT),
                    sessionItem(s, 102L, 3, result = AnswerResult.SKIPPED),
                )
            )

        val summary = service.findById(10L, 1L).summary!!

        assertEquals(3, summary.total)
        assertEquals(1, summary.correct)
        assertEquals(1, summary.incorrect)
        assertEquals(1, summary.skipped)
    }

    @Test
    fun `findById should throw NotFoundException when session does not belong to user`() {
        `when`(practiceSessionRepository.findByIdAndUserId(10L, 2L)).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            service.findById(10L, 2L)
        }
    }

    // SUBMIT ANSWER

    private fun stubSessionWithItems(
        s: PracticeSession,
        vararg items: PracticeSessionItem
    ) {
        `when`(practiceSessionRepository.findWithLockByIdAndUserId(10L, 1L))
            .thenReturn(s)
        `when`(practiceSessionItemRepository.findAllBySessionIdOrderByPositionAsc(10L))
            .thenReturn(items.toList())
    }

    @Test
    fun `submitAnswer should mark a matching answer correct`() {
        val s = session()
        val first = sessionItem(s, 100L, 1)
        val second = sessionItem(s, 101L, 2)

        stubSessionWithItems(s, first, second)

        val result = service.submitAnswer(10L, 1L, SubmitAnswerRequest(100L, "  TO EAT "))

        assertEquals(AnswerResult.CORRECT, result.result)
        assertEquals("To eat", result.correctDefinition)
        assertEquals(1, result.remaining)
        assertNull(result.summary)
        assertEquals(AnswerResult.CORRECT, first.result)
        assertEquals("  TO EAT ", first.submittedAnswer)
        assertNotNull(first.answeredAt)
        assertEquals(SessionStatus.IN_PROGRESS, s.status)
    }

    @Test
    fun `submitAnswer should mark a wrong answer incorrect and reveal the definition`() {
        val s = session()

        stubSessionWithItems(s, sessionItem(s, 100L, 1), sessionItem(s, 101L, 2))

        val result = service.submitAnswer(10L, 1L, SubmitAnswerRequest(100L, "to drink"))

        assertEquals(AnswerResult.INCORRECT, result.result)
        assertEquals("To eat", result.correctDefinition)
    }

    @Test
    fun `submitAnswer should skip the question when answer is null`() {
        val s = session()
        val first = sessionItem(s, 100L, 1)

        stubSessionWithItems(s, first, sessionItem(s, 101L, 2))

        val result = service.submitAnswer(10L, 1L, SubmitAnswerRequest(100L, null))

        assertEquals(AnswerResult.SKIPPED, result.result)
        assertNull(first.submittedAnswer)
        assertNotNull(first.answeredAt)
    }

    @Test
    fun `submitAnswer should complete the session when the last question is answered`() {
        val s = session()

        stubSessionWithItems(
            s,
            sessionItem(s, 100L, 1, result = AnswerResult.CORRECT),
            sessionItem(s, 101L, 2, result = AnswerResult.INCORRECT),
            sessionItem(s, 102L, 3),
        )

        val result = service.submitAnswer(10L, 1L, SubmitAnswerRequest(102L, "eat"))

        assertEquals(0, result.remaining)
        assertEquals(SessionStatus.COMPLETED, s.status)
        assertNotNull(s.endedAt)
        verify(practiceSessionRepository).save(s)

        val summary = result.summary!!
        assertEquals(3, summary.total)
        assertEquals(2, summary.correct)
        assertEquals(1, summary.incorrect)
        assertEquals(0, summary.skipped)
    }

    @Test
    fun `submitAnswer should complete the session when the last question is skipped`() {
        val s = session()

        stubSessionWithItems(
            s,
            sessionItem(s, 100L, 1, result = AnswerResult.CORRECT),
            sessionItem(s, 101L, 2),
        )

        val result = service.submitAnswer(10L, 1L, SubmitAnswerRequest(101L, null))

        assertEquals(SessionStatus.COMPLETED, s.status)
        assertEquals(1, result.summary!!.skipped)
    }

    @Test
    fun `submitAnswer should lock the session`() {
        val s = session()

        stubSessionWithItems(s, sessionItem(s, 100L, 1), sessionItem(s, 101L, 2))

        service.submitAnswer(10L, 1L, SubmitAnswerRequest(100L, "eat"))

        verify(practiceSessionRepository).findWithLockByIdAndUserId(10L, 1L)
    }

    @Test
    fun `submitAnswer should throw NotFoundException when session does not exist`() {
        `when`(practiceSessionRepository.findWithLockByIdAndUserId(10L, 1L))
            .thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            service.submitAnswer(10L, 1L, SubmitAnswerRequest(100L, "eat"))
        }
    }

    @Test
    fun `submitAnswer should throw ConflictException when session has ended`() {
        for (status in listOf(SessionStatus.COMPLETED, SessionStatus.ABANDONED)) {
            val s = session(status = status)

            `when`(practiceSessionRepository.findWithLockByIdAndUserId(10L, 1L))
                .thenReturn(s)

            assertThrows(ConflictException::class.java) {
                service.submitAnswer(10L, 1L, SubmitAnswerRequest(100L, "eat"))
            }
        }
    }

    @Test
    fun `submitAnswer should throw NotFoundException when item is not part of the session`() {
        val s = session()

        stubSessionWithItems(s, sessionItem(s, 100L, 1))

        assertThrows(NotFoundException::class.java) {
            service.submitAnswer(10L, 1L, SubmitAnswerRequest(999L, "eat"))
        }
    }

    @Test
    fun `submitAnswer should throw ConflictException when item is already answered`() {
        val s = session()

        stubSessionWithItems(
            s,
            sessionItem(s, 100L, 1, result = AnswerResult.CORRECT),
            sessionItem(s, 101L, 2),
        )

        assertThrows(ConflictException::class.java) {
            service.submitAnswer(10L, 1L, SubmitAnswerRequest(100L, "eat"))
        }
    }

    // ABANDON

    @Test
    fun `abandon should end an open session as abandoned`() {
        val s = session()

        `when`(practiceSessionRepository.findWithLockByIdAndUserId(10L, 1L))
            .thenReturn(s)
        `when`(practiceSessionRepository.save(any(PracticeSession::class.java)))
            .thenAnswer { it.getArgument<PracticeSession>(0) }
        `when`(practiceSessionItemRepository.findAllBySessionIdOrderByPositionAsc(10L))
            .thenReturn(listOf(sessionItem(s, 100L, 1)))

        val result = service.abandon(10L, 1L)

        assertEquals(SessionStatus.ABANDONED, result.status)
        assertNotNull(result.endedAt)
        assertNull(result.summary)
    }

    @Test
    fun `abandon should throw ConflictException when session has already ended`() {
        `when`(practiceSessionRepository.findWithLockByIdAndUserId(10L, 1L))
            .thenReturn(session(status = SessionStatus.COMPLETED))

        assertThrows(ConflictException::class.java) {
            service.abandon(10L, 1L)
        }
    }

    @Test
    fun `abandon should throw NotFoundException when session does not exist`() {
        `when`(practiceSessionRepository.findWithLockByIdAndUserId(10L, 2L))
            .thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            service.abandon(10L, 2L)
        }
    }
}
