package com.undrift.agent

import android.util.Log
import com.undrift.network.ChatMessage
import com.undrift.network.ConduitClient
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray

/**
 * Uses [ConduitClient] to assess user context via the Cloudflare AI Proxy,
 * falling back to [LocalContextAwareAgent] when offline or on error.
 */
class ProxyContextAwareAgent(
    private val client: ConduitClient = ConduitClient(),
    private val fallback: ContextAwareAgent = LocalContextAwareAgent.instance
) : ContextAwareAgent {

    override fun assessContext(input: ContextAssessmentInput): ContextAssessmentOutput = runBlocking {
        try {
            val response = client.chatCompletion(
                messages = listOf(
                    ChatMessage(
                        "system",
                        "You are the Context-Aware Agent for Undrift, an anti-procrastination Android application.\n" +
                        "Your responsibility is to understand the user's current behavioral context and determine whether their current activity is consistent with their intended focus.\n" +
                        "A blocked application being opened is NOT automatic proof that intervention is required. Context and persistence matter.\n" +
                        "If the user is typing (isTyping = true), they might be engaged in an important task or replying to a message; you should likely SUPPRESS the intervention.\n" +
                        "If they are idling or scrolling without typing (isTyping = false) in a distracting app, you should issue a personalized ELIGIBLE intervention.\n" +
                        "Return strictly valid JSON matching this schema:\n" +
                        "{\n" +
                        "  \"context\": \"FOCUS | STUDY | WORK | BREAK | SCHEDULED_ACTIVITY | CASUAL | IDLE | UNKNOWN\",\n" +
                        "  \"context_confidence\": 0.0 to 1.0,\n" +
                        "  \"current_activity\": \"string\",\n" +
                        "  \"activity_compatibility\": \"CONSISTENT | INCONSISTENT | POTENTIALLY_INCONSISTENT | UNKNOWN\",\n" +
                        "  \"distraction_confidence\": 0.0 to 1.0,\n" +
                        "  \"blocked\": true/false,\n" +
                        "  \"episode\": {\n" +
                        "    \"active\": true/false,\n" +
                        "    \"started_at\": timestamp_or_null,\n" +
                        "    \"duration_seconds\": integer\n" +
                        "  },\n" +
                        "  \"intervention\": {\n" +
                        "    \"state\": \"NOT_ELIGIBLE | WAITING | ELIGIBLE | SUPPRESSED\",\n" +
                        "    \"threshold_seconds\": integer,\n" +
                        "    \"elapsed_seconds\": integer,\n" +
                        "    \"remaining_seconds\": integer,\n" +
                        "    \"reason\": \"string\"\n" +
                        "  },\n" +
                        "  \"transition\": \"string or null\",\n" +
                        "  \"evidence\": [\"short string\"]\n" +
                        "}"
                    ),
                    ChatMessage("user", input.toPrompt())
                ),
                temperature = 0.2
            )
            parseOutput(response) ?: fallback.assessContext(input)
        } catch (e: Exception) {
            runCatching { Log.w(TAG, "Proxy context assessment failed; using local rules", e) }
            fallback.assessContext(input)
        }
    }

    private fun parseOutput(raw: String): ContextAssessmentOutput? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null

        return runCatching {
            val json = JSONObject(raw.substring(start, end + 1))
            val episodeJson = json.getJSONObject("episode")
            val interventionJson = json.getJSONObject("intervention")
            val evidenceArray = json.optJSONArray("evidence") ?: JSONArray()
            val evidenceList = List(evidenceArray.length()) { evidenceArray.getString(it) }

            ContextAssessmentOutput(
                context = UserContext.valueOf(json.getString("context")),
                contextConfidence = json.getDouble("context_confidence"),
                currentActivity = json.getString("current_activity"),
                activityCompatibility = ActivityCompatibility.valueOf(json.getString("activity_compatibility")),
                distractionConfidence = json.getDouble("distraction_confidence"),
                blocked = json.getBoolean("blocked"),
                episode = EpisodeInfo(
                    active = episodeJson.getBoolean("active"),
                    startedAt = if (episodeJson.isNull("started_at")) null else episodeJson.optLong("started_at"),
                    durationSeconds = episodeJson.getLong("duration_seconds")
                ),
                intervention = InterventionInfo(
                    state = InterventionState.valueOf(interventionJson.getString("state")),
                    thresholdSeconds = interventionJson.getLong("threshold_seconds"),
                    elapsedSeconds = interventionJson.getLong("elapsed_seconds"),
                    remainingSeconds = interventionJson.getLong("remaining_seconds"),
                    reason = interventionJson.getString("reason")
                ),
                transition = if (json.isNull("transition")) null else json.getString("transition"),
                evidence = evidenceList
            )
        }.getOrNull()
    }

    private fun ContextAssessmentInput.toPrompt(): String = JSONObject().apply {
        put("packageName", packageName)
        putNullable("appCategory", appCategory)
        put("isBlocked", isBlocked)
        putNullable("windowTitle", windowTitle)
        put("isFocusModeActive", isFocusModeActive)
        putNullable("focusSessionPlannedDuration", focusSessionPlannedDuration)
        putNullable("focusSessionStartTime", focusSessionStartTime)
        putNullable("activeGoal", activeGoal)
        putNullable("sessionStartTime", sessionStartTime)
        put("timeSpentMillis", timeSpentMillis)
        put("recentAppHistory", JSONArray(recentAppHistory))
        putNullable("previousContext", previousContext?.name)
        putNullable("previousContextConfidence", previousContextConfidence)
        putNullable("timeSinceLastIntervention", timeSinceLastIntervention)
        putNullable("configuredNudgeDelay", configuredNudgeDelay)
        put("isBreakState", isBreakState)
        put("isTyping", isTyping)
    }.toString()

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    companion object {
        private const val TAG = "ProxyContextAwareAgent"
    }
}
