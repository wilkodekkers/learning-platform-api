package com.wilkodekkers.paneng_api.practice.dto

import com.wilkodekkers.paneng_api.practice.SessionStatus
import java.time.Instant

data class PracticeSessionResponse(
    val id: Long,
    val bookId: Long,
    val chapterId: Long,
    val status: SessionStatus,
    val startedAt: Instant,
    val endedAt: Instant?,
    val totalItems: Int,
    val remaining: Int,
    val items: List<PracticeSessionItemResponse>,
    val summary: PracticeSessionSummary?,
)
