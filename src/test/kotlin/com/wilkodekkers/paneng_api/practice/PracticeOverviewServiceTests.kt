package com.wilkodekkers.paneng_api.practice

import com.wilkodekkers.paneng_api.books.Book
import com.wilkodekkers.paneng_api.books.BookRepository
import com.wilkodekkers.paneng_api.chapters.Chapter
import com.wilkodekkers.paneng_api.chapters.ChapterRepository
import com.wilkodekkers.paneng_api.common.exceptions.NotFoundException
import com.wilkodekkers.paneng_api.users.User
import com.wilkodekkers.paneng_api.vocabulary.VocabularyItem
import com.wilkodekkers.paneng_api.vocabulary.VocabularyItemRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.Optional

class PracticeOverviewServiceTests {

    private val bookRepository = mock(BookRepository::class.java)
    private val chapterRepository = mock(ChapterRepository::class.java)
    private val vocabularyItemRepository =
        mock(VocabularyItemRepository::class.java)
    private val practiceSessionItemRepository =
        mock(PracticeSessionItemRepository::class.java)

    private val service = PracticeOverviewService(
        bookRepository,
        chapterRepository,
        vocabularyItemRepository,
        practiceSessionItemRepository
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

    private val chapter1 = Chapter(id = 1L, book = book, title = "Intro", position = 1)
    private val chapter2 = Chapter(id = 2L, book = book, title = "Food", position = 2)
    private val chapter3 = Chapter(id = 3L, book = book, title = "Empty", position = 3)

    private val earlier = Instant.parse("2025-01-01T10:00:00Z")
    private val later = Instant.parse("2025-02-01T10:00:00Z")

    private fun itemCount(chapterId: Long, total: Long) =
        object : ChapterItemCount {
            override val chapterId = chapterId
            override val totalItems = total
        }

    private fun chapterStats(
        chapterId: Long,
        practiced: Long,
        correct: Long,
        incorrect: Long,
        skipped: Long,
        lastPracticedAt: Instant?
    ) = object : ChapterPracticeStats {
        override val chapterId = chapterId
        override val practicedItems = practiced
        override val correctAnswers = correct
        override val incorrectAnswers = incorrect
        override val skippedAnswers = skipped
        override val lastPracticedAt = lastPracticedAt
    }

    private fun itemStats(
        itemId: Long,
        correct: Long,
        incorrect: Long,
        skipped: Long,
        lastPracticedAt: Instant?
    ) = object : ItemPracticeStats {
        override val vocabularyItemId = itemId
        override val correctAnswers = correct
        override val incorrectAnswers = incorrect
        override val skippedAnswers = skipped
        override val lastPracticedAt = lastPracticedAt
    }

    private fun vocabularyItem(id: Long, term: String, chapter: Chapter) =
        VocabularyItem(
            id = id,
            chapter = chapter,
            term = term,
            definition = "definition",
            answers = "definition"
        )

    // BOOK OVERVIEW

    @Test
    fun `findBookOverview should combine item counts and practice stats per chapter`() {
        `when`(bookRepository.findByIdAndUserId(1L, 1L))
            .thenReturn(Optional.of(book))
        `when`(chapterRepository.findAllByBookIdAndBookUserIdOrderByPositionAsc(1L, 1L))
            .thenReturn(listOf(chapter1, chapter2, chapter3))
        `when`(vocabularyItemRepository.countItemsPerChapter(1L, 1L))
            .thenReturn(listOf(itemCount(1L, 4), itemCount(2L, 3)))
        `when`(practiceSessionItemRepository.findChapterStats(1L, 1L))
            .thenReturn(
                listOf(
                    chapterStats(1L, 3, 5, 2, 1, later),
                    chapterStats(2L, 1, 0, 1, 0, earlier),
                )
            )

        val result = service.findBookOverview(1L, 1L)

        assertEquals(1L, result.bookId)
        assertEquals(7, result.totalItems)
        assertEquals(4, result.practicedItems)
        assertEquals(3, result.neverPracticedItems)
        assertEquals(5L, result.correctAnswers)
        assertEquals(3L, result.incorrectAnswers)
        assertEquals(1L, result.skippedAnswers)
        assertEquals(later, result.lastPracticedAt)

        val first = result.chapters[0]
        assertEquals(1L, first.chapterId)
        assertEquals("Intro", first.title)
        assertEquals(4, first.totalItems)
        assertEquals(3, first.practicedItems)
        assertEquals(1, first.neverPracticedItems)
        assertEquals(later, first.lastPracticedAt)

        val second = result.chapters[1]
        assertEquals(3, second.totalItems)
        assertEquals(1, second.practicedItems)
        assertEquals(2, second.neverPracticedItems)
        assertEquals(earlier, second.lastPracticedAt)
    }

    @Test
    fun `findBookOverview should report an unpracticed chapter with zeros and no date`() {
        `when`(bookRepository.findByIdAndUserId(1L, 1L))
            .thenReturn(Optional.of(book))
        `when`(chapterRepository.findAllByBookIdAndBookUserIdOrderByPositionAsc(1L, 1L))
            .thenReturn(listOf(chapter1, chapter3))
        `when`(vocabularyItemRepository.countItemsPerChapter(1L, 1L))
            .thenReturn(listOf(itemCount(1L, 2)))
        `when`(practiceSessionItemRepository.findChapterStats(1L, 1L))
            .thenReturn(emptyList())

        val result = service.findBookOverview(1L, 1L)

        assertNull(result.lastPracticedAt)
        assertEquals(2, result.totalItems)
        assertEquals(0, result.practicedItems)
        assertEquals(2, result.neverPracticedItems)

        val empty = result.chapters[1]
        assertEquals(0, empty.totalItems)
        assertEquals(0, empty.neverPracticedItems)
        assertNull(empty.lastPracticedAt)
        assertEquals(0L, empty.correctAnswers)
    }

    @Test
    fun `findBookOverview should return an empty overview for a book without chapters`() {
        `when`(bookRepository.findByIdAndUserId(1L, 1L))
            .thenReturn(Optional.of(book))
        `when`(chapterRepository.findAllByBookIdAndBookUserIdOrderByPositionAsc(1L, 1L))
            .thenReturn(emptyList())
        `when`(vocabularyItemRepository.countItemsPerChapter(1L, 1L))
            .thenReturn(emptyList())
        `when`(practiceSessionItemRepository.findChapterStats(1L, 1L))
            .thenReturn(emptyList())

        val result = service.findBookOverview(1L, 1L)

        assertEquals(0, result.totalItems)
        assertEquals(emptyList<Any>(), result.chapters)
        assertNull(result.lastPracticedAt)
    }

    @Test
    fun `findBookOverview should throw NotFoundException when book does not belong to user`() {
        `when`(bookRepository.findByIdAndUserId(1L, 2L))
            .thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) {
            service.findBookOverview(1L, 2L)
        }
    }

    // CHAPTER OVERVIEW

    @Test
    fun `findChapterOverview should include never practiced items with zeros`() {
        `when`(chapterRepository.findByIdAndBookIdAndBookUserId(1L, 1L, 1L))
            .thenReturn(chapter1)
        `when`(
            vocabularyItemRepository
                .findAllByChapterIdAndChapterBookUserIdOrderByIdAsc(1L, 1L)
        ).thenReturn(
            listOf(
                vocabularyItem(1L, "กิน", chapter1),
                vocabularyItem(2L, "ดื่ม", chapter1),
                vocabularyItem(3L, "นอน", chapter1),
            )
        )
        `when`(practiceSessionItemRepository.findItemStats(1L, 1L))
            .thenReturn(
                listOf(
                    itemStats(1L, 2, 1, 0, later),
                    itemStats(2L, 0, 0, 1, earlier),
                )
            )

        val result = service.findChapterOverview(1L, 1L, 1L)

        assertEquals(1L, result.chapterId)
        assertEquals("Intro", result.title)
        assertEquals(3, result.totalItems)
        assertEquals(2, result.practicedItems)
        assertEquals(1, result.neverPracticedItems)
        assertEquals(2L, result.correctAnswers)
        assertEquals(1L, result.incorrectAnswers)
        assertEquals(1L, result.skippedAnswers)
        assertEquals(later, result.lastPracticedAt)

        assertEquals(later, result.items[0].lastPracticedAt)
        assertEquals(2L, result.items[0].correctAnswers)
        assertEquals(earlier, result.items[1].lastPracticedAt)
        assertEquals(1L, result.items[1].skippedAnswers)

        val never = result.items[2]
        assertEquals("นอน", never.term)
        assertNull(never.lastPracticedAt)
        assertEquals(0L, never.correctAnswers)
        assertEquals(0L, never.incorrectAnswers)
        assertEquals(0L, never.skippedAnswers)
    }

    @Test
    fun `findChapterOverview should handle a chapter without items`() {
        `when`(chapterRepository.findByIdAndBookIdAndBookUserId(3L, 1L, 1L))
            .thenReturn(chapter3)
        `when`(
            vocabularyItemRepository
                .findAllByChapterIdAndChapterBookUserIdOrderByIdAsc(3L, 1L)
        ).thenReturn(emptyList())
        `when`(practiceSessionItemRepository.findItemStats(3L, 1L))
            .thenReturn(emptyList())

        val result = service.findChapterOverview(1L, 3L, 1L)

        assertEquals(0, result.totalItems)
        assertEquals(0, result.neverPracticedItems)
        assertNull(result.lastPracticedAt)
    }

    @Test
    fun `findChapterOverview should throw NotFoundException when chapter does not belong to user`() {
        `when`(chapterRepository.findByIdAndBookIdAndBookUserId(1L, 1L, 2L))
            .thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            service.findChapterOverview(1L, 1L, 2L)
        }
    }
}
