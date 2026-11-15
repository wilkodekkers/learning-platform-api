package com.wilkodekkers.paneng_api.practice.dto

data class PracticeSessionSummary(
    val total: Int,
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
)
