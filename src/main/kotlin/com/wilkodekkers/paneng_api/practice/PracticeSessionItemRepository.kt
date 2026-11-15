package com.wilkodekkers.paneng_api.practice

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PracticeSessionItemRepository :
    JpaRepository<PracticeSessionItem, Long> {
    fun findAllBySessionIdOrderByPositionAsc(
        sessionId: Long
    ): List<PracticeSessionItem>

    // Only answered rows count as practiced. Rows whose vocabulary item has
    // been deleted are left out, as there is no item to attach them to.
    @Query(
        """
        select c.id as chapterId,
               count(distinct v.id) as practicedItems,
               count(case when i.result = AnswerResult.CORRECT then 1 end) as correctAnswers,
               count(case when i.result = AnswerResult.INCORRECT then 1 end) as incorrectAnswers,
               count(case when i.result = AnswerResult.SKIPPED then 1 end) as skippedAnswers,
               max(i.answeredAt) as lastPracticedAt
        from PracticeSessionItem i
        join i.vocabularyItem v
        join v.chapter c
        where i.answeredAt is not null
          and c.book.id = :bookId
          and c.book.user.id = :userId
        group by c.id
        """
    )
    fun findChapterStats(bookId: Long, userId: Long): List<ChapterPracticeStats>

    @Query(
        """
        select v.id as vocabularyItemId,
               count(case when i.result = AnswerResult.CORRECT then 1 end) as correctAnswers,
               count(case when i.result = AnswerResult.INCORRECT then 1 end) as incorrectAnswers,
               count(case when i.result = AnswerResult.SKIPPED then 1 end) as skippedAnswers,
               max(i.answeredAt) as lastPracticedAt
        from PracticeSessionItem i
        join i.vocabularyItem v
        where i.answeredAt is not null
          and v.chapter.id = :chapterId
          and v.chapter.book.user.id = :userId
        group by v.id
        """
    )
    fun findItemStats(chapterId: Long, userId: Long): List<ItemPracticeStats>
}
