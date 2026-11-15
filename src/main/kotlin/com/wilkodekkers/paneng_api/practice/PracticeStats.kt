package com.wilkodekkers.paneng_api.practice

import java.time.Instant

interface ChapterPracticeStats {
    val chapterId: Long
    val practicedItems: Long
    val correctAnswers: Long
    val incorrectAnswers: Long
    val skippedAnswers: Long
    val lastPracticedAt: Instant?
}

interface ItemPracticeStats {
    val vocabularyItemId: Long
    val correctAnswers: Long
    val incorrectAnswers: Long
    val skippedAnswers: Long
    val lastPracticedAt: Instant?
}

interface ChapterItemCount {
    val chapterId: Long
    val totalItems: Long
}
