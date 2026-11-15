package com.wilkodekkers.paneng_api.practice

import com.wilkodekkers.paneng_api.vocabulary.VocabularyItem
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "practice_session_items")
class PracticeSessionItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: PracticeSession,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_item_id")
    val vocabularyItem: VocabularyItem?,

    @Column(nullable = false)
    val position: Int,

    @Column(name = "term_snapshot", nullable = false)
    val termSnapshot: String,

    @Column(
        name = "definition_snapshot",
        nullable = false,
        columnDefinition = "TEXT"
    )
    val definitionSnapshot: String,

    @Column(
        name = "answers_snapshot",
        nullable = false,
        columnDefinition = "TEXT"
    )
    val answersSnapshot: String,

    @Column(name = "submitted_answer", columnDefinition = "TEXT")
    var submittedAnswer: String? = null,

    @Enumerated(EnumType.STRING)
    var result: AnswerResult? = null,

    @Column(name = "answered_at")
    var answeredAt: Instant? = null,
)
