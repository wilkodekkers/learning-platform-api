package com.wilkodekkers.paneng_api.practice.dto

import com.wilkodekkers.paneng_api.practice.AnswerResult

data class SubmitAnswerResponse(
    val result: AnswerResult,
    val correctDefinition: String,
    val remaining: Int,
    val summary: PracticeSessionSummary?,
)
