package com.wilkodekkers.paneng_api.practice.dto

import com.wilkodekkers.paneng_api.practice.AnswerResult

/**
 * The correct definition is only included once the question is answered, so
 * the client never receives the answer to an open question.
 */
data class PracticeSessionItemResponse(
    val id: Long,
    val position: Int,
    val term: String,
    val answered: Boolean,
    val result: AnswerResult?,
    val submittedAnswer: String?,
    val correctDefinition: String?,
)
