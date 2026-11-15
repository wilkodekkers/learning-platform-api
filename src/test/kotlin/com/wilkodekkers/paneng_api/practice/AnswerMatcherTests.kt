package com.wilkodekkers.paneng_api.practice

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnswerMatcherTests {

    @Test
    fun `matches should accept an exact answer`() {
        assertTrue(AnswerMatcher.matches("to eat", "to eat"))
    }

    @Test
    fun `matches should accept any of the alternatives`() {
        val answers = "eat; to eat; have a meal"

        assertTrue(AnswerMatcher.matches("eat", answers))
        assertTrue(AnswerMatcher.matches("to eat", answers))
        assertTrue(AnswerMatcher.matches("have a meal", answers))
    }

    @Test
    fun `matches should ignore case`() {
        assertTrue(AnswerMatcher.matches("TO EAT", "To eat"))
    }

    @Test
    fun `matches should ignore surrounding and repeated whitespace`() {
        assertTrue(AnswerMatcher.matches("  to   eat  ", "to eat"))
    }

    @Test
    fun `matches should ignore surrounding punctuation`() {
        assertTrue(AnswerMatcher.matches("to eat.", "to eat"))
        assertTrue(AnswerMatcher.matches("\"to eat\"", "to eat"))
        assertTrue(AnswerMatcher.matches("to eat", "To eat!"))
    }

    @Test
    fun `matches should treat composed and decomposed accents as equal`() {
        assertTrue(AnswerMatcher.matches("café", "café"))
    }

    @Test
    fun `matches should not strip accents`() {
        assertFalse(AnswerMatcher.matches("cafe", "café"))
    }

    @Test
    fun `matches should reject a wrong answer`() {
        assertFalse(AnswerMatcher.matches("to drink", "eat; to eat"))
    }

    @Test
    fun `matches should reject a partial answer`() {
        assertFalse(AnswerMatcher.matches("to", "to eat"))
    }

    @Test
    fun `matches should not split on commas`() {
        assertTrue(AnswerMatcher.matches("to eat, to dine", "to eat, to dine"))
        assertFalse(AnswerMatcher.matches("to eat", "to eat, to dine"))
    }

    @Test
    fun `matches should reject a blank input`() {
        assertFalse(AnswerMatcher.matches("   ", "to eat"))
        assertFalse(AnswerMatcher.matches("", "to eat"))
    }

    @Test
    fun `matches should not let empty alternatives match a punctuation-only input`() {
        assertFalse(AnswerMatcher.matches("...", "eat;; ;"))
    }

    @Test
    fun `matches should keep Thai combining characters intact`() {
        assertTrue(AnswerMatcher.matches("กิน", "กิน; ทาน"))
        assertTrue(AnswerMatcher.matches("ทาน", "กิน; ทาน"))
        assertFalse(AnswerMatcher.matches("กิ", "กิน"))
    }
}
