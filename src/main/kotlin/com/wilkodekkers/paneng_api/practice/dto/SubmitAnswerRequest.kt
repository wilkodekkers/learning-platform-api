package com.wilkodekkers.paneng_api.practice.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class SubmitAnswerRequest(
    @field:NotNull
    val sessionItemId: Long,

    @field:Pattern(regexp = ".*\\S.*", message = "must not be blank")
    val answer: String? = null,
)
