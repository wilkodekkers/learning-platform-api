package com.wilkodekkers.paneng_api.practice

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface PracticeSessionRepository : JpaRepository<PracticeSession, Long> {
    fun findByIdAndUserId(id: Long, userId: Long): PracticeSession?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByIdAndUserId(id: Long, userId: Long): PracticeSession?

    fun findByUserIdAndStatus(
        userId: Long,
        status: SessionStatus
    ): PracticeSession?
}
