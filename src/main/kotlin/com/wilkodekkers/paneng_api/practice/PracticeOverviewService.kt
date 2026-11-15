package com.wilkodekkers.paneng_api.practice

import com.wilkodekkers.paneng_api.books.BookRepository
import com.wilkodekkers.paneng_api.chapters.ChapterRepository
import com.wilkodekkers.paneng_api.common.exceptions.NotFoundException
import com.wilkodekkers.paneng_api.practice.dto.BookPracticeOverviewResponse
import com.wilkodekkers.paneng_api.practice.dto.ChapterPracticeOverviewResponse
import com.wilkodekkers.paneng_api.practice.dto.PracticeOverviewChapterResponse
import com.wilkodekkers.paneng_api.practice.dto.PracticeOverviewItemResponse
import com.wilkodekkers.paneng_api.vocabulary.VocabularyItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PracticeOverviewService(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val vocabularyItemRepository: VocabularyItemRepository,
    private val practiceSessionItemRepository: PracticeSessionItemRepository,
) {

    @Transactional(readOnly = true)
    fun findBookOverview(
        bookId: Long,
        userId: Long
    ): BookPracticeOverviewResponse {
        bookRepository.findByIdAndUserId(bookId, userId).orElseThrow {
            NotFoundException("Book with ID $bookId not found")
        }

        val chapters = chapterRepository
            .findAllByBookIdAndBookUserIdOrderByPositionAsc(bookId, userId)

        val totalItems = vocabularyItemRepository
            .countItemsPerChapter(bookId, userId)
            .associate { it.chapterId to it.totalItems.toInt() }

        val stats = practiceSessionItemRepository
            .findChapterStats(bookId, userId)
            .associateBy { it.chapterId }

        val chapterOverviews = chapters.map { chapter ->
            val chapterStats = stats[chapter.id]
            val total = totalItems[chapter.id] ?: 0
            val practiced = chapterStats?.practicedItems?.toInt() ?: 0

            PracticeOverviewChapterResponse(
                chapterId = chapter.id,
                title = chapter.title,
                position = chapter.position,
                lastPracticedAt = chapterStats?.lastPracticedAt,
                totalItems = total,
                practicedItems = practiced,
                neverPracticedItems = total - practiced,
                correctAnswers = chapterStats?.correctAnswers ?: 0,
                incorrectAnswers = chapterStats?.incorrectAnswers ?: 0,
                skippedAnswers = chapterStats?.skippedAnswers ?: 0,
            )
        }

        val total = chapterOverviews.sumOf { it.totalItems }
        val practiced = chapterOverviews.sumOf { it.practicedItems }

        return BookPracticeOverviewResponse(
            bookId = bookId,
            lastPracticedAt = chapterOverviews
                .mapNotNull { it.lastPracticedAt }
                .maxOrNull(),
            totalItems = total,
            practicedItems = practiced,
            neverPracticedItems = total - practiced,
            correctAnswers = chapterOverviews.sumOf { it.correctAnswers },
            incorrectAnswers = chapterOverviews.sumOf { it.incorrectAnswers },
            skippedAnswers = chapterOverviews.sumOf { it.skippedAnswers },
            chapters = chapterOverviews,
        )
    }

    @Transactional(readOnly = true)
    fun findChapterOverview(
        bookId: Long,
        chapterId: Long,
        userId: Long
    ): ChapterPracticeOverviewResponse {
        val chapter = chapterRepository.findByIdAndBookIdAndBookUserId(
            chapterId,
            bookId,
            userId
        ) ?: throw NotFoundException(
            "Chapter with ID $chapterId not found in book with ID $bookId"
        )

        val stats = practiceSessionItemRepository
            .findItemStats(chapterId, userId)
            .associateBy { it.vocabularyItemId }

        val items = vocabularyItemRepository
            .findAllByChapterIdAndChapterBookUserIdOrderByIdAsc(
                chapterId,
                userId
            )
            .map { item ->
                val itemStats = stats[item.id]

                PracticeOverviewItemResponse(
                    vocabularyItemId = item.id,
                    term = item.term,
                    lastPracticedAt = itemStats?.lastPracticedAt,
                    correctAnswers = itemStats?.correctAnswers ?: 0,
                    incorrectAnswers = itemStats?.incorrectAnswers ?: 0,
                    skippedAnswers = itemStats?.skippedAnswers ?: 0,
                )
            }

        val practiced = items.count { it.lastPracticedAt != null }

        return ChapterPracticeOverviewResponse(
            chapterId = chapter.id,
            title = chapter.title,
            position = chapter.position,
            lastPracticedAt = items.mapNotNull { it.lastPracticedAt }.maxOrNull(),
            totalItems = items.size,
            practicedItems = practiced,
            neverPracticedItems = items.size - practiced,
            correctAnswers = items.sumOf { it.correctAnswers },
            incorrectAnswers = items.sumOf { it.incorrectAnswers },
            skippedAnswers = items.sumOf { it.skippedAnswers },
            items = items,
        )
    }
}
