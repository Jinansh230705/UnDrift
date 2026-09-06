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
    val isTyping: Boolean = false,
    val isDoomScrolling: Boolean = false,
    val isIdle: Boolean = false
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
        // Rule 1: Actively typing => strictly SUPPRESS intervention
        if (input.isTyping) {
            return ContextAssessmentOutput(
                context = UserContext.WORK,
                contextConfidence = 0.9,
                currentActivity = input.packageName,
                activityCompatibility = ActivityCompatibility.CONSISTENT,
                distractionConfidence = 0.0,
                blocked = input.isBlocked,
                episode = EpisodeInfo(true, input.sessionStartTime, (input.timeSpentMillis / 1000)),
                intervention = InterventionInfo(
                    state = InterventionState.SUPPRESSED,
                    thresholdSeconds = 0,
                    elapsedSeconds = (input.timeSpentMillis / 1000),
                    remainingSeconds = 0,
                    reason = "User is actively typing or engaged in an important task; intervention suppressed."
                ),
                transition = null,
                evidence = listOf("Active typing detected", "Intervention suppressed for user workflow")
            )
        }

        // Rule 2: Doom scrolling in restricted / focus context => ELIGIBLE
        if (input.isDoomScrolling && (input.isBlocked || input.isFocusModeActive)) {
            return ContextAssessmentOutput(
                context = if (input.isFocusModeActive) UserContext.FOCUS else UserContext.WORK,
                contextConfidence = 0.95,
                currentActivity = input.packageName,
                activityCompatibility = ActivityCompatibility.INCONSISTENT,
                distractionConfidence = 0.95,
                blocked = input.isBlocked,
                episode = EpisodeInfo(true, input.sessionStartTime, (input.timeSpentMillis / 1000)),
                intervention = InterventionInfo(
                    state = InterventionState.ELIGIBLE,
                    thresholdSeconds = 0,
                    elapsedSeconds = (input.timeSpentMillis / 1000),
                    remainingSeconds = 0,
                    reason = "Rapid doom scrolling detected in restricted application."
                ),
                transition = null,
                evidence = listOf("Rapid scroll events without text input", "Doom scrolling behavior")
            )
        }

        // Rule 3: Idle in restricted / focus context => ELIGIBLE
        if (input.isIdle && (input.isBlocked || input.isFocusModeActive)) {
            return ContextAssessmentOutput(
                context = UserContext.IDLE,
                contextConfidence = 0.85,
                currentActivity = input.packageName,
                activityCompatibility = ActivityCompatibility.INCONSISTENT,
                distractionConfidence = 0.85,
                blocked = input.isBlocked,
                episode = EpisodeInfo(true, input.sessionStartTime, (input.timeSpentMillis / 1000)),
                intervention = InterventionInfo(
                    state = InterventionState.ELIGIBLE,
                    thresholdSeconds = 0,
                    elapsedSeconds = (input.timeSpentMillis / 1000),
                    remainingSeconds = 0,
                    reason = "User is idle in restricted application."
                ),
                transition = null,
                evidence = listOf("User idle without interaction", "Restricted app feed idle")
            )
        }

        // Rule 4: Blocked application default fallback (safe offline fallback)
        return ContextAssessmentOutput(
            context = UserContext.UNKNOWN,
            contextConfidence = 0.0,
            currentActivity = "UNKNOWN",
            activityCompatibility = ActivityCompatibility.UNKNOWN,
            distractionConfidence = 0.0,
            blocked = input.isBlocked,
            episode = EpisodeInfo(false, null, 0),
            intervention = InterventionInfo(
                state = if (input.isBlocked) InterventionState.ELIGIBLE else InterventionState.NOT_ELIGIBLE,
                thresholdSeconds = 0,
                elapsedSeconds = 0,
                remainingSeconds = 0,
                reason = if (input.isBlocked) "Offline block fallback." else "Fallback safe mode; proxy analysis unavailable."
            ),
            transition = null,
            evidence = listOf("Local fallback used.")
        )
    }

    companion object {
        val instance: LocalContextAwareAgent by lazy { LocalContextAwareAgent() }
    }
}
