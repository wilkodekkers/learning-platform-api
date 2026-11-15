package com.wilkodekkers.paneng_api.practice

import java.text.Normalizer
import java.util.Locale

/**
 * Compares a typed answer against the accepted answers of a vocabulary item.
 * Accepted answers are separated by ";". Both sides are normalized first.
 */
object AnswerMatcher {

    private const val SEPARATOR = ";"

    private val whitespace = Regex("\\s+")

    private val punctuation = setOf(
        CharCategory.CONNECTOR_PUNCTUATION,
        CharCategory.DASH_PUNCTUATION,
        CharCategory.START_PUNCTUATION,
        CharCategory.END_PUNCTUATION,
        CharCategory.INITIAL_QUOTE_PUNCTUATION,
        CharCategory.FINAL_QUOTE_PUNCTUATION,
        CharCategory.OTHER_PUNCTUATION,
    )

    fun matches(input: String, answers: String): Boolean {
        val normalizedInput = normalize(input)

        if (normalizedInput.isEmpty()) {
            return false
        }

        return answers
            .split(SEPARATOR)
            .map(::normalize)
            .any { it.isNotEmpty() && it == normalizedInput }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC)
            .lowercase(Locale.ROOT)
            .replace(whitespace, " ")
            .trim { it.isWhitespace() || it.category in punctuation }
}
