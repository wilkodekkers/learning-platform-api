package com.wilkodekkers.paneng_api.practice

import com.wilkodekkers.paneng_api.common.exceptions.NotFoundException
import com.wilkodekkers.paneng_api.practice.dto.BookPracticeOverviewResponse
import com.wilkodekkers.paneng_api.practice.dto.ChapterPracticeOverviewResponse
import com.wilkodekkers.paneng_api.practice.dto.PracticeOverviewChapterResponse
import com.wilkodekkers.paneng_api.practice.dto.PracticeOverviewItemResponse
import com.wilkodekkers.paneng_api.security.CurrentUser
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant

@WebMvcTest(
    controllers = [PracticeOverviewController::class],
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        OAuth2ResourceServerAutoConfiguration::class
    ]
)
class PracticeOverviewControllerTests(
    @Autowired private val mockMvc: MockMvc
) {

    @MockitoBean
    private lateinit var practiceOverviewService: PracticeOverviewService

    @MockitoBean
    private lateinit var currentUser: CurrentUser

    private val practicedAt = Instant.parse("2025-01-01T10:00:00Z")

    // GET /api/books/{bookId}/practice-overview

    @Test
    fun `GET book practice overview should return the overview`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceOverviewService.findBookOverview(1L, 1L))
            .thenReturn(
                BookPracticeOverviewResponse(
                    bookId = 1L,
                    lastPracticedAt = practicedAt,
                    totalItems = 5,
                    practicedItems = 3,
                    neverPracticedItems = 2,
                    correctAnswers = 4,
                    incorrectAnswers = 2,
                    skippedAnswers = 1,
                    chapters = listOf(
                        PracticeOverviewChapterResponse(
                            chapterId = 1L,
                            title = "Intro",
                            position = 1,
                            lastPracticedAt = practicedAt,
                            totalItems = 5,
                            practicedItems = 3,
                            neverPracticedItems = 2,
                            correctAnswers = 4,
                            incorrectAnswers = 2,
                            skippedAnswers = 1,
                        )
                    )
                )
            )

        mockMvc.get("/api/books/1/practice-overview")
            .andExpect {
                status { isOk() }
                jsonPath("$.bookId") { value(1) }
                jsonPath("$.lastPracticedAt") { value("2025-01-01T10:00:00Z") }
                jsonPath("$.totalItems") { value(5) }
                jsonPath("$.neverPracticedItems") { value(2) }
                jsonPath("$.correctAnswers") { value(4) }
                jsonPath("$.chapters.length()") { value(1) }
                jsonPath("$.chapters[0].title") { value("Intro") }
                jsonPath("$.chapters[0].practicedItems") { value(3) }
            }
    }

    @Test
    fun `GET book practice overview should return null lastPracticedAt when never practiced`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceOverviewService.findBookOverview(1L, 1L))
            .thenReturn(
                BookPracticeOverviewResponse(1L, null, 0, 0, 0, 0, 0, 0, emptyList())
            )

        mockMvc.get("/api/books/1/practice-overview")
            .andExpect {
                status { isOk() }
                jsonPath("$.lastPracticedAt") { value(null) }
            }
    }

    @Test
    fun `GET book practice overview should return not found when book does not exist`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceOverviewService.findBookOverview(999L, 1L))
            .thenThrow(NotFoundException("Book with ID 999 not found"))

        mockMvc.get("/api/books/999/practice-overview")
            .andExpect { status { isNotFound() } }
    }

    // GET /api/books/{bookId}/chapters/{chapterId}/practice-overview

    @Test
    fun `GET chapter practice overview should return the overview with items`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceOverviewService.findChapterOverview(1L, 1L, 1L))
            .thenReturn(
                ChapterPracticeOverviewResponse(
                    chapterId = 1L,
                    title = "Intro",
                    position = 1,
                    lastPracticedAt = practicedAt,
                    totalItems = 2,
                    practicedItems = 1,
                    neverPracticedItems = 1,
                    correctAnswers = 1,
                    incorrectAnswers = 0,
                    skippedAnswers = 0,
                    items = listOf(
                        PracticeOverviewItemResponse(1L, "กิน", practicedAt, 1, 0, 0),
                        PracticeOverviewItemResponse(2L, "ดื่ม", null, 0, 0, 0),
                    )
                )
            )

        mockMvc.get("/api/books/1/chapters/1/practice-overview")
            .andExpect {
                status { isOk() }
                jsonPath("$.chapterId") { value(1) }
                jsonPath("$.neverPracticedItems") { value(1) }
                jsonPath("$.items.length()") { value(2) }
                jsonPath("$.items[0].term") { value("กิน") }
                jsonPath("$.items[0].correctAnswers") { value(1) }
                jsonPath("$.items[1].lastPracticedAt") { value(null) }
            }
    }

    @Test
    fun `GET chapter practice overview should return not found when chapter does not exist`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceOverviewService.findChapterOverview(1L, 999L, 1L))
            .thenThrow(NotFoundException("Chapter with ID 999 not found"))

        mockMvc.get("/api/books/1/chapters/999/practice-overview")
            .andExpect { status { isNotFound() } }
    }
}
