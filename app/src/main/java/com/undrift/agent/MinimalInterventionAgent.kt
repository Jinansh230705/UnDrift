package com.undrift.agent

enum class InterventionLevel {
    NONE,
    AWARENESS,
    REFLECTION,
    RETURN_TO_FOCUS
}

data class InterventionDecisionInput(
    val packageName: String,
    val contextAssessment: ContextAssessmentOutput,
    val isFocusModeActive: Boolean,
    val timeSpentMillis: Long,
    val timeLimitMillis: Long? = null,
    val lastInterventionTimestamp: Long = 0L,
    val cooldownMillis: Long = 60_000L,
    val declaredTask: String? = null,
    val recentInterventionsCount: Int = 0,
    val previousInterventionResponse: String? = null
)

data class InterventionDecisionOutput(
    val shouldIntervene: Boolean,
    val level: InterventionLevel,
    val message: String?,
    val reason: String?,
    val confidence: Double,
    val cooldownMinutes: Int
)

interface MinimalInterventionAgent {
    fun decideIntervention(input: InterventionDecisionInput): InterventionDecisionOutput
}

class LocalMinimalInterventionAgent : MinimalInterventionAgent {
    override fun decideIntervention(input: InterventionDecisionInput): InterventionDecisionOutput {
        val now = System.currentTimeMillis()
        val contextOutput = input.contextAssessment
        val state = contextOutput.intervention.state
        val userContext = contextOutput.context

        // Rule 1: Breaks, casual usage, or non-eligible states => NEVER intervene
        if (userContext == UserContext.BREAK || 
            userContext == UserContext.CASUAL || 
            state == InterventionState.SUPPRESSED || 
            state == InterventionState.NOT_ELIGIBLE || 
            state == InterventionState.WAITING) {
            return InterventionDecisionOutput(
                shouldIntervene = false,
                level = InterventionLevel.NONE,
                message = null,
                reason = contextOutput.intervention.reason.ifBlank { "Context does not warrant intervention." },
                confidence = contextOutput.contextConfidence,
                cooldownMinutes = 0
            )
        }

        // Rule 2: Repeatedly ignored interventions increase cooldown and threshold (Situation D)
        val isRepeatedlyIgnored = input.recentInterventionsCount > 0 && 
            (input.previousInterventionResponse.equals("ignored", ignoreCase = true) || 
             input.previousInterventionResponse.equals("dismissed", ignoreCase = true))

        val effectiveCooldownMillis = if (isRepeatedlyIgnored) {
            // Increase cooldown significantly if previously ignored (e.g. 15-20 min)
            (input.cooldownMillis * (input.recentInterventionsCount + 1)).coerceIn(600_000L, 1_800_000L)
        } else {
            input.cooldownMillis
        }

        val timeSinceLast = if (input.lastInterventionTimestamp > 0) now - input.lastInterventionTimestamp else Long.MAX_VALUE
        val isCooldownActive = timeSinceLast < effectiveCooldownMillis

        if (isCooldownActive) {
            val remainingMins = (((effectiveCooldownMillis - timeSinceLast) / 60_000L) + 1).toInt().coerceAtLeast(1)
            return InterventionDecisionOutput(
                shouldIntervene = false,
                level = InterventionLevel.NONE,
                message = null,
                reason = if (isRepeatedlyIgnored) {
                    "Intervention threshold raised and cooldown extended after ignored intervention."
                } else {
                    "Intervention cooldown active to prevent notification fatigue."
                },
                confidence = contextOutput.contextConfidence,
                cooldownMinutes = remainingMins
            )
        }

        // Rule 3: Brief activity (< 20 seconds) => NO INTERVENTION (Situation A)
        if (input.timeSpentMillis in 1..19_999L) {
            return InterventionDecisionOutput(
                shouldIntervene = false,
                level = InterventionLevel.NONE,
                message = null,
                reason = "Activity is brief; insufficient evidence of distraction.",
                confidence = 0.85,
                cooldownMinutes = 0
            )
        }

        // Rule 4: Scale intervention levels based on duration, context, and focus session
        val minutesSpent = (input.timeSpentMillis / 60_000L).toInt()
        val (level, message, suggestedCooldown) = when {
            // Situation D: Repeated ignored interventions should NOT escalate intensity; stay conservative
            isRepeatedlyIgnored -> Triple(
                InterventionLevel.AWARENESS,
                "Still on this?",
                20
            )
            // Level 3: Strong evidence in active focus session (Situation C / Level 3)
            input.isFocusModeActive && (minutesSpent >= 8 || input.timeSpentMillis >= 30_000L) && contextOutput.contextConfidence >= 0.8 -> Triple(
                InterventionLevel.RETURN_TO_FOCUS,
                "Your focus session is still running. Want to get back to it?",
                20
            )
            // Level 2: Reflection (Situation C / Level 2)
            minutesSpent >= 5 || input.isFocusModeActive -> Triple(
                InterventionLevel.REFLECTION,
                "Want to get back to what you were working on?",
                15
            )
            // Level 1: Awareness (Situation B / Level 1)
            else -> Triple(
                InterventionLevel.AWARENESS,
                "You've been here for a while.",
                10
            )
        }

        return InterventionDecisionOutput(
            shouldIntervene = true,
            level = level,
            message = message,
            reason = contextOutput.intervention.reason.ifBlank { "Activity inconsistent with intended focus." },
            confidence = contextOutput.contextConfidence.coerceAtLeast(0.8),
            cooldownMinutes = suggestedCooldown
        )
    }

    companion object {
        val instance: LocalMinimalInterventionAgent by lazy { LocalMinimalInterventionAgent() }
    }
}
