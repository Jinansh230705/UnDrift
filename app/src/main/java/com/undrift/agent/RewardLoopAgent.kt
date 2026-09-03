package com.undrift.agent

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
    val eventId: String, // Unique identifier for the event to prevent duplicates
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

interface RewardLoopAgent {
    fun evaluate(input: RewardEventInput): RewardOutput
}

class LocalRewardLoopAgent : RewardLoopAgent {
    private val processedEventIds = mutableSetOf<String>()

    override fun evaluate(input: RewardEventInput): RewardOutput {
        // Prevent processing duplicate events
        if (processedEventIds.contains(input.eventId)) {
            return RewardOutput(RewardType.NONE, RewardMagnitude.LOW, null)
        }
        processedEventIds.add(input.eventId)

        // Trivial events should not be rewarded
        if (input.event == "APP_OPENED" || input.event == "BUTTON_CLICKED" || input.event == "TIMER_STARTED") {
            return RewardOutput(RewardType.NONE, RewardMagnitude.LOW, null)
        }

        if (input.event == "DISTRACTION_RECOVERED" || input.event == "RECOVERY") {
            return RewardOutput(
                type = RewardType.RECOVERY,
                magnitude = RewardMagnitude.MEDIUM,
                message = "Nice recovery. You got back to your task."
            )
        }

        if (input.event == "FOCUS_SESSION_COMPLETED") {
            val planned = input.plannedDurationMinutes ?: 0
            val actual = input.actualFocusDurationMinutes ?: 0

            // Milestones (e.g., reaching 120 minutes of focus in a day)
            if (input.dailyFocusMinutes >= 120 && input.previousRewardType != RewardType.MILESTONE) {
                return RewardOutput(
                    type = RewardType.MILESTONE,
                    magnitude = RewardMagnitude.HIGH,
                    message = "You've reached 2 hours of focus today."
                )
            }

            // Consistency (e.g., building a streak)
            if (input.currentStreak > 0 && input.currentStreak > input.previousStreak && input.currentStreak % 3 == 0) {
                return RewardOutput(
                    type = RewardType.CONSISTENCY,
                    magnitude = RewardMagnitude.HIGH,
                    message = "Another focused session done."
                )
            }

            // Full completion
            if (actual >= planned && planned > 0) {
                return RewardOutput(
                    type = RewardType.SESSION_COMPLETION,
                    magnitude = RewardMagnitude.MEDIUM,
                    message = "Focus session complete."
                )
            }

            // Partial completion but meaningful progress (e.g., at least 5 minutes)
            if (actual in 5 until planned) {
                return RewardOutput(
                    type = RewardType.PROGRESS,
                    magnitude = RewardMagnitude.LOW,
                    message = "Good effort. Every bit of focus counts."
                )
            }

            // Session was too short or no meaningful progress
            return RewardOutput(RewardType.NONE, RewardMagnitude.LOW, null)
        }

        return RewardOutput(RewardType.NONE, RewardMagnitude.LOW, null)
    }
}
