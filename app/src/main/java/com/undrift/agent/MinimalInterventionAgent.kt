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

        if (input.contextAssessment.context == UserContext.IMPORTANT_TASK) {
            return InterventionDecisionOutput(
                shouldIntervene = false,
                level = InterventionLevel.NONE,
                reason = "User is engaged in an important task. Cooldown / bypass active.",
                cooldownActive = isCooldownActive
            )
        }

        if (input.isFocusModeActive && input.contextAssessment.context == UserContext.POTENTIAL_DISTRACTION) {
            if (isCooldownActive) {
                return InterventionDecisionOutput(
                    shouldIntervene = false,
                    level = InterventionLevel.NONE,
                    reason = "Intervention cooldown active to prevent spamming the user.",
                    cooldownActive = true
                )
            }

            return InterventionDecisionOutput(
                shouldIntervene = true,
                level = InterventionLevel.STRICT_OVERLAY,
                reason = "Focus mode is active and user accessed a distracting application.",
                cooldownActive = false
            )
        }

        val limit = input.timeLimitMillis ?: 0L
        if (limit > 0 && input.timeSpentMillis >= limit) {
            if (isCooldownActive) {
                return InterventionDecisionOutput(
                    shouldIntervene = false,
                    level = InterventionLevel.NONE,
                    reason = "Daily app limit reached, but intervention cooldown active.",
                    cooldownActive = true
                )
            }

            return InterventionDecisionOutput(
                shouldIntervene = true,
                level = InterventionLevel.STRICT_OVERLAY,
                reason = "Daily application usage limit exceeded.",
                cooldownActive = false
            )
        }

        return InterventionDecisionOutput(
            shouldIntervene = false,
            level = InterventionLevel.NONE,
            reason = "No intervention necessary.",
            cooldownActive = isCooldownActive
        )
    }

    companion object {
        val instance: LocalMinimalInterventionAgent by lazy { LocalMinimalInterventionAgent() }
    }
}
