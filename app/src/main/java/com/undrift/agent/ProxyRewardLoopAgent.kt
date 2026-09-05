package com.undrift.agent

import android.util.Log
import com.undrift.network.ChatMessage
import com.undrift.network.ProxyAiClient
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Uses [ProxyAiClient] for AI-generated reward evaluations via Cloudflare AI Proxy,
 * while keeping reward decisions safe and available offline through [fallback].
 * All evaluations are logged to [RewardRepository] for UI state reactivity.
 */
class ProxyRewardLoopAgent(
    private val client: ProxyAiClient = ProxyAiClient(),
    private val fallback: LocalRewardLoopAgent = LocalRewardLoopAgent.instance,
    private val repository: RewardRepository = RewardRepository.instance
) : RewardLoopAgent {

    override fun evaluate(input: RewardEventInput): RewardOutput = runBlocking {
        if (repository.isDuplicate(input.eventId)) {
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
            val parsedOutput = parseOutput(response)
            if (parsedOutput != null) {
                repository.markProcessed(input.eventId)
                val record = RewardEvaluationRecord(input = input, output = parsedOutput)
                repository.addRecord(record)
                return@runBlocking parsedOutput
            }
        } catch (error: Exception) {
            runCatching { Log.w(TAG, "Proxy reward evaluation failed; using local rules", error) }
        }

        // Fallback to local evaluation rules which marks processed and logs to repository
        return@runBlocking fallback.evaluate(input)
    }

    override fun getRecentEvaluations(): List<RewardEvaluationRecord> {
        return repository.getRecentRecords()
    }

    override fun clearEvaluations() {
        repository.clear()
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
