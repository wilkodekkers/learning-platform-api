package com.wilkodekkers.paneng_api.practice.dto

import java.time.Instant

data class ChapterPracticeOverviewResponse(
    val chapterId: Long,
    val title: String,
    val position: Int,
    val lastPracticedAt: Instant?,
    val totalItems: Int,
    val practicedItems: Int,
    val neverPracticedItems: Int,
    val correctAnswers: Long,
    val incorrectAnswers: Long,
    val skippedAnswers: Long,
    val items: List<PracticeOverviewItemResponse>,
)
