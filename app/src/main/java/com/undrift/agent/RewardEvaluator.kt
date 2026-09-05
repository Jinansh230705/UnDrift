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

        if (input.event == "FOCUS_TIME_ACCRUED" || input.event == "FOCUS_USAGE_PROGRESS") {
            val minutes = input.actualFocusDurationMinutes ?: 0
            if (minutes >= 5) {
                val magnitude = when {
                    minutes >= 60 -> RewardMagnitude.HIGH
                    minutes >= 20 -> RewardMagnitude.MEDIUM
                    else -> RewardMagnitude.LOW
                }
                return RewardOutput(
                    type = RewardType.PROGRESS,
                    magnitude = magnitude,
                    message = "Earned focus coins for ${minutes}m spent in focus mode!"
                )
            }
            return RewardOutput(RewardType.NONE, RewardMagnitude.LOW, null)
        }

        if (input.event == "FOCUS_SESSION_COMPLETED") {
            val planned = input.plannedDurationMinutes ?: 0
            val actual = input.actualFocusDurationMinutes ?: 0

            // 1. Milestones (e.g. reaching 120 minutes of focus in a day or planned 2h session)
            if ((input.dailyFocusMinutes >= 120 || planned >= 120 || actual >= 120) && input.previousRewardType != RewardType.MILESTONE) {
                return RewardOutput(
                    type = RewardType.MILESTONE,
                    magnitude = RewardMagnitude.HIGH,
                    message = "You've reached 2 hours of focus today."
                )
            }

            // 2. Consistency (e.g. building a streak)
            if (input.currentStreak > 0 && input.currentStreak > input.previousStreak && input.currentStreak % 3 == 0) {
                return RewardOutput(
                    type = RewardType.CONSISTENCY,
                    magnitude = RewardMagnitude.HIGH,
                    message = "Another focused session done."
                )
            }

            // 3. Full session completion (e.g. 10m, 25m, 60m, 120m sessions)
            if (actual >= planned && planned > 0) {
                val magnitude = when {
                    planned >= 60 -> RewardMagnitude.HIGH
                    planned >= 20 -> RewardMagnitude.MEDIUM
                    else -> RewardMagnitude.LOW
                }
                return RewardOutput(
                    type = RewardType.SESSION_COMPLETION,
                    magnitude = magnitude,
                    message = "Focus session complete."
                )
            }

            // 4. Partial completion but meaningful progress (e.g. at least 5 minutes)
            if (actual >= 5) {
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
