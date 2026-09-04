package com.undrift.agent

class RewardEvaluator {

    fun evaluateEvent(input: RewardEventInput): RewardOutput {
        if (RewardPolicy.isTrivialInteraction(input.event)) {
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

            return RewardOutput(RewardType.NONE, RewardMagnitude.LOW, null)
        }

        return RewardOutput(RewardType.NONE, RewardMagnitude.LOW, null)
    }
}
