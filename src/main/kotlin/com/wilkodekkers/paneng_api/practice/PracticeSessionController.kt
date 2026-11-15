package com.wilkodekkers.paneng_api.practice

import com.wilkodekkers.paneng_api.practice.dto.SubmitAnswerRequest
import com.wilkodekkers.paneng_api.security.CurrentUser
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Tag(name = "Practice", description = "Endpoints for practicing chapter vocabulary")
@SecurityRequirement(name = "bearerAuth")
class PracticeSessionController(
    private val practiceSessionService: PracticeSessionService,
    private val currentUser: CurrentUser,
) {

    @PostMapping("/books/{bookId}/chapters/{chapterId}/practice-sessions")
    fun start(
        @PathVariable bookId: Long,
        @PathVariable chapterId: Long
    ): ResponseEntity<*> {
        val result = practiceSessionService.start(
            bookId,
            chapterId,
            currentUser.getId()
        )

        val status = if (result.created) HttpStatus.CREATED else HttpStatus.OK

        return ResponseEntity.status(status).body(result.session)
    }

    @GetMapping("/practice-sessions/current")
    fun findCurrent() = practiceSessionService.findCurrent(currentUser.getId())

    @GetMapping("/practice-sessions/{sessionId}")
    fun findById(@PathVariable sessionId: Long) =
        practiceSessionService.findById(sessionId, currentUser.getId())

    @PostMapping("/practice-sessions/{sessionId}/answers")
    fun submitAnswer(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: SubmitAnswerRequest
    ) = practiceSessionService.submitAnswer(
        sessionId,
        currentUser.getId(),
        request
    )

    @PostMapping("/practice-sessions/{sessionId}/abandon")
    fun abandon(@PathVariable sessionId: Long) =
        practiceSessionService.abandon(sessionId, currentUser.getId())
}
