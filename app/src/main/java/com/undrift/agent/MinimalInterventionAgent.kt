package com.undrift.agent

enum class InterventionLevel {
    NONE,
    SOFT_NUDGE,
    STRICT_OVERLAY
}

data class InterventionDecisionInput(
    val packageName: String,
    val contextAssessment: ContextAssessmentOutput,
    val isFocusModeActive: Boolean,
    val timeSpentMillis: Long,
    val timeLimitMillis: Long? = null,
    val lastInterventionTimestamp: Long = 0L,
    val cooldownMillis: Long = 60_000L
)

data class InterventionDecisionOutput(
    val shouldIntervene: Boolean,
    val level: InterventionLevel,
    val reason: String?,
    val cooldownActive: Boolean
)

interface MinimalInterventionAgent {
    fun decideIntervention(input: InterventionDecisionInput): InterventionDecisionOutput
}

class LocalMinimalInterventionAgent : MinimalInterventionAgent {
    override fun decideIntervention(input: InterventionDecisionInput): InterventionDecisionOutput {
        val now = System.currentTimeMillis()
        val timeSinceLast = now - input.lastInterventionTimestamp
        val isCooldownActive = timeSinceLast < input.cooldownMillis

        val state = input.contextAssessment.intervention.state

        if (state == InterventionState.SUPPRESSED || state == InterventionState.NOT_ELIGIBLE || state == InterventionState.WAITING) {
            return InterventionDecisionOutput(
                shouldIntervene = false,
                level = InterventionLevel.NONE,
                reason = input.contextAssessment.intervention.reason,
                cooldownActive = isCooldownActive
            )
        }

        // State is ELIGIBLE
        if (isCooldownActive) {
            return InterventionDecisionOutput(
                shouldIntervene = false,
                level = InterventionLevel.NONE,
                reason = "Intervention cooldown active to prevent spamming the user.",
                cooldownActive = true
            )
        }

        // Determine level
        val level = if (input.isFocusModeActive) {
            InterventionLevel.STRICT_OVERLAY
        } else {
            InterventionLevel.SOFT_NUDGE
        }

        return InterventionDecisionOutput(
            shouldIntervene = true,
            level = level,
            reason = input.contextAssessment.intervention.reason,
            cooldownActive = false
        )
    }

    companion object {
        val instance: LocalMinimalInterventionAgent by lazy { LocalMinimalInterventionAgent() }
    }
}
