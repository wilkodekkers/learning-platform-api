package com.wilkodekkers.paneng_api.practice

import com.wilkodekkers.paneng_api.security.CurrentUser
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/books/{bookId}")
@Tag(name = "Practice overview", description = "Endpoints for practice history of books and chapters")
@SecurityRequirement(name = "bearerAuth")
class PracticeOverviewController(
    private val practiceOverviewService: PracticeOverviewService,
    private val currentUser: CurrentUser,
) {

    @GetMapping("/practice-overview")
    fun findBookOverview(@PathVariable bookId: Long) =
        practiceOverviewService.findBookOverview(bookId, currentUser.getId())

    @GetMapping("/chapters/{chapterId}/practice-overview")
    fun findChapterOverview(
        @PathVariable bookId: Long,
        @PathVariable chapterId: Long
    ) = practiceOverviewService.findChapterOverview(
        bookId,
        chapterId,
        currentUser.getId()
    )
}
