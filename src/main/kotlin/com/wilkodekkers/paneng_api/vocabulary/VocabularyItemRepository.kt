package com.wilkodekkers.paneng_api.vocabulary

import com.wilkodekkers.paneng_api.practice.ChapterItemCount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VocabularyItemRepository : JpaRepository<VocabularyItem, Long> {
    fun findAllByChapterIdAndChapterBookUserIdOrderByIdAsc(
        chapterId: Long,
        userId: Long
    ): List<VocabularyItem>

    fun findByIdAndChapterIdAndChapterBookIdAndChapterBookUserId(
        id: Long,
        chapterId: Long,
        bookId: Long,
        userId: Long
    ): VocabularyItem?

    @Query(
        """
        select v.chapter.id as chapterId, count(v) as totalItems
        from VocabularyItem v
        where v.chapter.book.id = :bookId
          and v.chapter.book.user.id = :userId
        group by v.chapter.id
        """
    )
    fun countItemsPerChapter(bookId: Long, userId: Long): List<ChapterItemCount>
}
