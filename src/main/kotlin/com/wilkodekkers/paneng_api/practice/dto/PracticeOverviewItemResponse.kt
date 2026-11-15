package com.wilkodekkers.paneng_api.practice.dto

import java.time.Instant

data class PracticeOverviewItemResponse(
    val vocabularyItemId: Long,
    val term: String,
    val lastPracticedAt: Instant?,
    val correctAnswers: Long,
    val incorrectAnswers: Long,
    val skippedAnswers: Long,
)
