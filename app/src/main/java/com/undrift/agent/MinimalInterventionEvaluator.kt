package com.undrift.agent

import java.util.Locale

class MinimalInterventionEvaluator {

    fun evaluate(input: MinimalInterventionInput): MinimalInterventionOutput {
        // 1. Break Protection - Breaks are never treated as failures or distractions
        if (input.isBreak || input.context.equals("BREAK", ignoreCase = true)) {
            return MinimalInterventionOutput.noIntervention(
                reason = "User is on a break. Silence preserves autonomy and recovery.",
                confidence = 0.95,
                cooldownMinutes = 0
            )
        }

        // 2. Task Compatibility - Do not intervene if activity aligns with focus
        if (input.activityCompatibility == ActivityCompatibility.CONSISTENT) {
            return MinimalInterventionOutput.noIntervention(
                reason = "Current activity is consistent with the user's declared task or focus session.",
                confidence = 0.90,
                cooldownMinutes = 0
            )
        }

        // 3. Low Context Confidence - Do not intervene on uncertain signals
        if (input.contextConfidence < 0.60) {
            val formattedConf = String.format(Locale.US, "%.2f", input.contextConfidence)
            return MinimalInterventionOutput.noIntervention(
                reason = "Context confidence ($formattedConf) is too low to justify intervention.",
                confidence = input.contextConfidence,
                cooldownMinutes = 0
            )
        }

        // 4. Persistence Check - Brief interactions do not warrant intervention
        if (input.sessionDurationMinutes < 2) {
            return MinimalInterventionOutput.noIntervention(
                reason = "Activity duration (${input.sessionDurationMinutes}m) is brief. Insufficient evidence of sustained distraction.",
                confidence = 0.85,
                cooldownMinutes = 0
            )
        }

        // 5. Cooldown Calculation & Suppression
        val requiredCooldown = calculateCooldown(input)
        if (input.minutesSinceLastIntervention < requiredCooldown) {
            val remaining = requiredCooldown - input.minutesSinceLastIntervention
            return MinimalInterventionOutput.noIntervention(
                reason = "Intervention cooldown active ($remaining min remaining) to prevent notification fatigue.",
                confidence = 0.90,
                cooldownMinutes = remaining
            )
        }

        // 6. Repeated Distraction / Ignored Intervention Backoff
        val wasPreviouslyIgnored = input.previousInterventionResponse == InterventionResponse.IGNORED ||
                input.previousInterventionResponse == InterventionResponse.DISMISSED
        
        if (wasPreviouslyIgnored && input.sessionDurationMinutes < 6) {
            return MinimalInterventionOutput.noIntervention(
                reason = "Previous intervention was ignored; system has become more conservative with higher evidence threshold.",
                confidence = 0.85,
                cooldownMinutes = requiredCooldown
            )
        }

        // 7. Context Assessment outside focus session
        if (!input.focusSessionActive && input.activityCompatibility != ActivityCompatibility.INCONSISTENT) {
            return MinimalInterventionOutput.noIntervention(
                reason = "No active focus session and activity is not explicitly inconsistent.",
                confidence = 0.80,
                cooldownMinutes = 0
            )
        }

        // 8. Decide Intervention Level (Lowest level that could reasonably help)
        val level = determineLevel(input, wasPreviouslyIgnored)
        if (level == InterventionLevel.NONE) {
            return MinimalInterventionOutput.noIntervention(
                reason = "Silence is preferable given current context and user autonomy.",
                confidence = 0.80,
                cooldownMinutes = 0
            )
        }

        val message = generateMessage(level, input)
        val sanitizedMessage = sanitizeMessage(message)
        val cooldown = calculatePostInterventionCooldown(level, wasPreviouslyIgnored, input.recentInterventions)

        return MinimalInterventionOutput(
            intervene = true,
            level = level.value,
            message = sanitizedMessage,
            reason = generateReason(level, input),
            confidence = calculateOutputConfidence(input, level),
            cooldownMinutes = cooldown
        )
    }

    private fun determineLevel(
        input: MinimalInterventionInput,
        wasPreviouslyIgnored: Boolean
    ): InterventionLevel {
        // Level 3: Strongest nudge, requires active focus session, high confidence, and persistent duration
        if (input.focusSessionActive && input.contextConfidence >= 0.85 && input.sessionDurationMinutes >= 10) {
            return InterventionLevel.RETURN_TO_FOCUS
        }

        // Level 2: Reflection, encourage the user to reconsider
        if (input.sessionDurationMinutes >= 6 || (input.focusSessionActive && input.sessionDurationMinutes >= 5)) {
            return InterventionLevel.REFLECTION
        }

        // Level 1: Awareness, neutral notice of behavior
        if (input.sessionDurationMinutes >= 2) {
            return InterventionLevel.AWARENESS
        }

        return InterventionLevel.NONE
    }

    private fun generateMessage(level: InterventionLevel, input: MinimalInterventionInput): String {
        val task = input.declaredTask?.trim()
        return when (level) {
            InterventionLevel.RETURN_TO_FOCUS -> {
                if (!task.isNullOrBlank()) {
                    "Your focus session on '$task' is still running. Want to get back to it?"
                } else {
                    "Your focus session is still running. Want to get back to it?"
                }
            }
            InterventionLevel.REFLECTION -> {
                if (!task.isNullOrBlank()) {
                    "Ready to return to '$task'?"
                } else {
                    "Want to get back to what you were working on?"
                }
            }
            InterventionLevel.AWARENESS -> {
                if (input.sessionDurationMinutes >= 5) {
                    "You've been here for a while."
                } else {
                    "You've been on this for a bit."
                }
            }
            InterventionLevel.NONE -> ""
        }
    }

    private fun generateReason(level: InterventionLevel, input: MinimalInterventionInput): String {
        return when (level) {
            InterventionLevel.RETURN_TO_FOCUS ->
                "Active focus session running with persistent activity inconsistent with declared task."
            InterventionLevel.REFLECTION ->
                "Activity duration (${input.sessionDurationMinutes}m) suggests drift during focus."
            InterventionLevel.AWARENESS ->
                "Gentle awareness signal for initial inconsistent activity."
            InterventionLevel.NONE ->
                "Silence is more helpful right now."
        }
    }

    private fun calculateCooldown(input: MinimalInterventionInput): Int {
        val wasIgnored = input.previousInterventionResponse == InterventionResponse.IGNORED ||
                input.previousInterventionResponse == InterventionResponse.DISMISSED

        return when {
            wasIgnored && input.recentInterventions >= 2 -> 45
            wasIgnored -> 30
            input.recentInterventions >= 3 -> 35
            input.recentInterventions >= 1 -> 20
            else -> 15
        }
    }

    private fun calculatePostInterventionCooldown(
        level: InterventionLevel,
        wasPreviouslyIgnored: Boolean,
        recentInterventions: Int
    ): Int {
        var base = when (level) {
            InterventionLevel.RETURN_TO_FOCUS -> 25
            InterventionLevel.REFLECTION -> 20
            InterventionLevel.AWARENESS -> 15
            InterventionLevel.NONE -> 0
        }

        if (wasPreviouslyIgnored) {
            base += 15 // Scale up cooldown when ignored
        }
        if (recentInterventions >= 2) {
            base += 10
        }
        return base
    }

    private fun calculateOutputConfidence(input: MinimalInterventionInput, level: InterventionLevel): Double {
        val base = when (level) {
            InterventionLevel.RETURN_TO_FOCUS -> 0.90
            InterventionLevel.REFLECTION -> 0.85
            InterventionLevel.AWARENESS -> 0.80
            InterventionLevel.NONE -> 0.75
        }
        return (base * (0.5 + input.contextConfidence * 0.5)).coerceIn(0.5, 0.98)
    }

    private fun sanitizeMessage(message: String): String {
        val lower = message.lowercase()
        for (forbidden in FORBIDDEN_WORDS) {
            if (lower.contains(forbidden)) {
                return "You've been on this for a bit."
            }
        }
        return message
    }

    companion object {
        private val FORBIDDEN_WORDS = listOf(
            "waste",
            "wasting",
            "procrastinat",
            "lazy",
            "failed",
            "failure",
            "disappoint",
            "get off",
            "stop using",
            "need to focus",
            "shame",
            "punish",
            "threat"
        )
    }
}
