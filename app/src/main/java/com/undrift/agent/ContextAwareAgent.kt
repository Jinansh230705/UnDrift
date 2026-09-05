package com.undrift.agent

enum class UserContext {
    FOCUS,
    STUDY,
    WORK,
    BREAK,
    CASUAL,
    SCHEDULED_ACTIVITY,
    IDLE,
    UNKNOWN
}

enum class ActivityCompatibility {
    CONSISTENT,
    INCONSISTENT,
    POTENTIALLY_INCONSISTENT,
    UNKNOWN
}

enum class InterventionState {
    NOT_ELIGIBLE,
    WAITING,
    ELIGIBLE,
    SUPPRESSED
}

data class EpisodeInfo(
    val active: Boolean,
    val startedAt: Long?,
    val durationSeconds: Long
)

data class InterventionInfo(
    val state: InterventionState,
    val thresholdSeconds: Long,
    val elapsedSeconds: Long,
    val remainingSeconds: Long,
    val reason: String
)

data class ContextAssessmentInput(
    val packageName: String,
    val appCategory: String? = null,
    val isBlocked: Boolean = false,
    val windowTitle: String? = null,
    val isFocusModeActive: Boolean = false,
    val focusSessionPlannedDuration: Long? = null,
    val focusSessionStartTime: Long? = null,
    val activeGoal: String? = null,
    val sessionStartTime: Long? = null,
    val timeSpentMillis: Long = 0L,
    val recentAppHistory: List<String> = emptyList(),
    val previousContext: UserContext? = null,
    val previousContextConfidence: Double? = null,
    val timeSinceLastIntervention: Long? = null,
    val configuredNudgeDelay: Long? = null,
    val isBreakState: Boolean = false,
    val isTyping: Boolean = false
)

data class ContextAssessmentOutput(
    val context: UserContext,
    val contextConfidence: Double,
    val currentActivity: String,
    val activityCompatibility: ActivityCompatibility,
    val distractionConfidence: Double,
    val blocked: Boolean,
    val episode: EpisodeInfo,
    val intervention: InterventionInfo,
    val transition: String?,
    val evidence: List<String>
)

interface ContextAwareAgent {
    fun assessContext(input: ContextAssessmentInput): ContextAssessmentOutput
}

class LocalContextAwareAgent : ContextAwareAgent {
    override fun assessContext(input: ContextAssessmentInput): ContextAssessmentOutput {
        // A very safe, conservative fallback that fails closed (never nudging by default)
        return ContextAssessmentOutput(
            context = UserContext.UNKNOWN,
            contextConfidence = 0.0,
            currentActivity = "UNKNOWN",
            activityCompatibility = ActivityCompatibility.UNKNOWN,
            distractionConfidence = 0.0,
            blocked = input.isBlocked,
            episode = EpisodeInfo(false, null, 0),
            intervention = InterventionInfo(
                state = InterventionState.NOT_ELIGIBLE,
                thresholdSeconds = 0,
                elapsedSeconds = 0,
                remainingSeconds = 0,
                reason = "Fallback safe mode; proxy analysis unavailable."
            ),
            transition = null,
            evidence = listOf("Local fallback used.")
        )
    }

    companion object {
        val instance: LocalContextAwareAgent by lazy { LocalContextAwareAgent() }
    }
}
