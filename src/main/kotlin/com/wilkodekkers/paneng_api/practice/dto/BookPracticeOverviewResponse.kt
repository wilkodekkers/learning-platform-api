package com.wilkodekkers.paneng_api.practice.dto

import java.time.Instant

data class BookPracticeOverviewResponse(
    val bookId: Long,
    val lastPracticedAt: Instant?,
    val totalItems: Int,
    val practicedItems: Int,
    val neverPracticedItems: Int,
    val correctAnswers: Long,
    val incorrectAnswers: Long,
    val skippedAnswers: Long,
    val chapters: List<PracticeOverviewChapterResponse>,
)
