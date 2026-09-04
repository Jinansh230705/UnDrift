package com.undrift.agent

import android.util.Log
import com.undrift.network.ChatMessage
import com.undrift.network.ProxyAiClient
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.Collections

/**
 * Uses the proxy for agent-generated reward messaging while keeping reward
 * decisions safe and available offline through [fallback].
 */
class ProxyRewardLoopAgent(
    private val client: ProxyAiClient,
    private val fallback: LocalRewardLoopAgent = LocalRewardLoopAgent()
) : RewardLoopAgent {
    private val processedEventIds = Collections.synchronizedSet(mutableSetOf<String>())
    override fun evaluate(input: RewardEventInput): RewardOutput = runBlocking {
        if (!processedEventIds.add(input.eventId)) {
            return@runBlocking RewardOutput(RewardType.NONE, RewardMagnitude.LOW, null)
        }

        try {
            val response = client.chatCompletion(
                messages = listOf(
                    ChatMessage(
                        "system",
                        "You are UnDrift's reward agent. Return only valid JSON with exactly " +
                            "type (NONE, SESSION_COMPLETION, PROGRESS, RECOVERY, MILESTONE, " +
                            "CONSISTENCY), magnitude (LOW, MEDIUM, HIGH), and message (string or null). " +
                            "Never invent points or reward types."
                    ),
                    ChatMessage("user", input.toPrompt())
                ),
                temperature = 0.2
            )
            parseOutput(response) ?: fallback.evaluate(input)
        } catch (error: Exception) {
            Log.w(TAG, "Proxy reward evaluation failed; using local rules", error)
            fallback.evaluate(input)
        }
    }

    private fun parseOutput(raw: String): RewardOutput? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null

        return runCatching {
            val json = JSONObject(raw.substring(start, end + 1))
            val type = RewardType.valueOf(json.getString("type"))
            val magnitude = RewardMagnitude.valueOf(json.getString("magnitude"))
            val message = if (json.isNull("message")) null else json.optString("message")
            RewardOutput(type, magnitude, message)
        }.getOrNull()
    }

    private fun RewardEventInput.toPrompt(): String = buildString {
        append("Evaluate this event: ")
        append(JSONObject().apply {
            put("eventId", eventId)
            put("event", event)
            putNullable("plannedDurationMinutes", plannedDurationMinutes)
            putNullable("actualFocusDurationMinutes", actualFocusDurationMinutes)
            put("distractionEvents", distractionEvents)
            put("successfulRecoveries", successfulRecoveries)
            put("currentStreak", currentStreak)
            put("previousStreak", previousStreak)
            put("dailyFocusMinutes", dailyFocusMinutes)
            put("weeklyFocusMinutes", weeklyFocusMinutes)
            put("goalProgress", goalProgress)
            putNullable("previousRewardType", previousRewardType?.name)
        }.toString())
    }

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    companion object {
        private const val TAG = "ProxyRewardLoopAgent"
    }
}
