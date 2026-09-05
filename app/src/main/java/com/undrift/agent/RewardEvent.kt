package com.undrift.agent

import java.util.UUID

enum class RewardType {
    NONE,
    SESSION_COMPLETION,
    PROGRESS,
    RECOVERY,
    MILESTONE,
    CONSISTENCY
}

enum class RewardMagnitude {
    LOW,
    MEDIUM,
    HIGH
}

data class RewardEventInput(
    val eventId: String = UUID.randomUUID().toString(),
    val event: String,
    val plannedDurationMinutes: Int? = null,
    val actualFocusDurationMinutes: Int? = null,
    val distractionEvents: Int = 0,
    val successfulRecoveries: Int = 0,
    val currentStreak: Int = 0,
    val previousStreak: Int = 0,
    val dailyFocusMinutes: Int = 0,
    val weeklyFocusMinutes: Int = 0,
    val goalProgress: Double = 0.0,
    val previousRewardType: RewardType? = null
)

data class RewardOutput(
    val type: RewardType,
    val magnitude: RewardMagnitude,
    val message: String?
)

data class RewardEvaluationRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val input: RewardEventInput,
    val output: RewardOutput
)
