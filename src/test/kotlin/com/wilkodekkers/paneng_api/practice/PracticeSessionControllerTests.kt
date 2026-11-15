package com.wilkodekkers.paneng_api.practice

import com.wilkodekkers.paneng_api.common.exceptions.ConflictException
import com.wilkodekkers.paneng_api.common.exceptions.NotFoundException
import com.wilkodekkers.paneng_api.practice.dto.PracticeSessionItemResponse
import com.wilkodekkers.paneng_api.practice.dto.PracticeSessionResponse
import com.wilkodekkers.paneng_api.practice.dto.PracticeSessionSummary
import com.wilkodekkers.paneng_api.practice.dto.SubmitAnswerRequest
import com.wilkodekkers.paneng_api.practice.dto.SubmitAnswerResponse
import com.wilkodekkers.paneng_api.security.CurrentUser
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant

@WebMvcTest(
    controllers = [PracticeSessionController::class],
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        OAuth2ResourceServerAutoConfiguration::class
    ]
)
class PracticeSessionControllerTests(
    @Autowired private val mockMvc: MockMvc
) {

    @MockitoBean
    private lateinit var practiceSessionService: PracticeSessionService

    @MockitoBean
    private lateinit var currentUser: CurrentUser

    private val session = PracticeSessionResponse(
        id = 10L,
        bookId = 1L,
        chapterId = 1L,
        status = SessionStatus.IN_PROGRESS,
        startedAt = Instant.parse("2025-01-01T10:00:00Z"),
        endedAt = null,
        totalItems = 2,
        remaining = 2,
        items = listOf(
            PracticeSessionItemResponse(100L, 1, "กิน", false, null, null, null),
            PracticeSessionItemResponse(101L, 2, "ดื่ม", false, null, null, null),
        ),
        summary = null
    )

    // POST /api/books/{bookId}/chapters/{chapterId}/practice-sessions

    @Test
    fun `POST practice session should return created for a new session`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.start(1L, 1L, 1L))
            .thenReturn(StartSessionResult(session, created = true))

        mockMvc.post("/api/books/1/chapters/1/practice-sessions")
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(10) }
                jsonPath("$.status") { value("IN_PROGRESS") }
                jsonPath("$.items.length()") { value(2) }
                jsonPath("$.items[0].term") { value("กิน") }
                jsonPath("$.items[0].correctDefinition") { value(null) }
            }
    }

    @Test
    fun `POST practice session should return ok when resuming an open session`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.start(1L, 1L, 1L))
            .thenReturn(StartSessionResult(session, created = false))

        mockMvc.post("/api/books/1/chapters/1/practice-sessions")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(10) }
            }
    }

    @Test
    fun `POST practice session should return not found when chapter does not exist`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.start(1L, 999L, 1L))
            .thenThrow(NotFoundException("Chapter with ID 999 not found"))

        mockMvc.post("/api/books/1/chapters/999/practice-sessions")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `POST practice session should return conflict when chapter has no items`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.start(1L, 1L, 1L))
            .thenThrow(ConflictException("No vocabulary items to practice"))

        mockMvc.post("/api/books/1/chapters/1/practice-sessions")
            .andExpect { status { isConflict() } }
    }

    // GET /api/practice-sessions/current

    @Test
    fun `GET current practice session should return the open session`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.findCurrent(1L)).thenReturn(session)

        mockMvc.get("/api/practice-sessions/current")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(10) }
            }
    }

    @Test
    fun `GET current practice session should return not found when none is open`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.findCurrent(1L))
            .thenThrow(NotFoundException("No open practice session"))

        mockMvc.get("/api/practice-sessions/current")
            .andExpect { status { isNotFound() } }
    }

    // GET /api/practice-sessions/{sessionId}

    @Test
    fun `GET practice session should return the session`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.findById(10L, 1L)).thenReturn(session)

        mockMvc.get("/api/practice-sessions/10")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(10) }
                jsonPath("$.remaining") { value(2) }
            }
    }

    @Test
    fun `GET practice session should return not found when session does not exist`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.findById(999L, 1L))
            .thenThrow(NotFoundException("Practice session with ID 999 not found"))

        mockMvc.get("/api/practice-sessions/999")
            .andExpect { status { isNotFound() } }
    }

    // POST /api/practice-sessions/{sessionId}/answers

    @Test
    fun `POST answer should return the graded result`() {
        val request = SubmitAnswerRequest(sessionItemId = 100L, answer = "to eat")

        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.submitAnswer(10L, 1L, request))
            .thenReturn(
                SubmitAnswerResponse(
                    result = AnswerResult.CORRECT,
                    correctDefinition = "To eat",
                    remaining = 1,
                    summary = null
                )
            )

        mockMvc.post("/api/practice-sessions/10/answers") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"sessionItemId": 100, "answer": "to eat"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.result") { value("CORRECT") }
            jsonPath("$.correctDefinition") { value("To eat") }
            jsonPath("$.remaining") { value(1) }
        }

        verify(practiceSessionService).submitAnswer(10L, 1L, request)
    }

    @Test
    fun `POST answer should include the summary when the session completes`() {
        val request = SubmitAnswerRequest(sessionItemId = 101L, answer = "to drink")

        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.submitAnswer(10L, 1L, request))
            .thenReturn(
                SubmitAnswerResponse(
                    result = AnswerResult.INCORRECT,
                    correctDefinition = "To drink (formal)",
                    remaining = 0,
                    summary = PracticeSessionSummary(2, 1, 1, 0)
                )
            )

        mockMvc.post("/api/practice-sessions/10/answers") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"sessionItemId": 101, "answer": "to drink"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.remaining") { value(0) }
            jsonPath("$.summary.total") { value(2) }
            jsonPath("$.summary.correct") { value(1) }
        }
    }

    @Test
    fun `POST answer should treat a missing answer as a skip`() {
        val request = SubmitAnswerRequest(sessionItemId = 100L, answer = null)

        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.submitAnswer(10L, 1L, request))
            .thenReturn(
                SubmitAnswerResponse(AnswerResult.SKIPPED, "To eat", 1, null)
            )

        mockMvc.post("/api/practice-sessions/10/answers") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"sessionItemId": 100}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.result") { value("SKIPPED") }
        }
    }

    @Test
    fun `POST answer should return bad request when answer is blank`() {
        mockMvc.post("/api/practice-sessions/10/answers") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"sessionItemId": 100, "answer": "   "}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST answer should return bad request when sessionItemId is missing`() {
        mockMvc.post("/api/practice-sessions/10/answers") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"answer": "to eat"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST answer should return conflict when item is already answered`() {
        val request = SubmitAnswerRequest(sessionItemId = 100L, answer = "to eat")

        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.submitAnswer(10L, 1L, request))
            .thenThrow(ConflictException("Already answered"))

        mockMvc.post("/api/practice-sessions/10/answers") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"sessionItemId": 100, "answer": "to eat"}"""
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST answer should return not found when session does not exist`() {
        val request = SubmitAnswerRequest(sessionItemId = 100L, answer = "to eat")

        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.submitAnswer(999L, 1L, request))
            .thenThrow(NotFoundException("Practice session with ID 999 not found"))

        mockMvc.post("/api/practice-sessions/999/answers") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"sessionItemId": 100, "answer": "to eat"}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    // POST /api/practice-sessions/{sessionId}/abandon

    @Test
    fun `POST abandon should return the abandoned session`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.abandon(10L, 1L))
            .thenReturn(session.copy(status = SessionStatus.ABANDONED))

        mockMvc.post("/api/practice-sessions/10/abandon")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("ABANDONED") }
            }
    }

    @Test
    fun `POST abandon should return conflict when session has already ended`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.abandon(10L, 1L))
            .thenThrow(ConflictException("Already ended"))

        mockMvc.post("/api/practice-sessions/10/abandon")
            .andExpect { status { isConflict() } }
    }

    @Test
    fun `POST abandon should return not found when session does not exist`() {
        `when`(currentUser.getId()).thenReturn(1L)
        `when`(practiceSessionService.abandon(999L, 1L))
            .thenThrow(NotFoundException("Practice session with ID 999 not found"))

        mockMvc.post("/api/practice-sessions/999/abandon")
            .andExpect { status { isNotFound() } }
    }
}
