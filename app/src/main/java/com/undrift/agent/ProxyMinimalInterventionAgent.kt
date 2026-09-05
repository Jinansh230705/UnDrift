package com.undrift.agent

import android.util.Log
import com.undrift.network.ChatMessage
import com.undrift.network.ProxyAiClient
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Uses [ProxyAiClient] to decide on interventions via the Cloudflare AI Proxy,
 * falling back to [LocalMinimalInterventionAgent] when offline or on error.
 */
class ProxyMinimalInterventionAgent(
    private val client: ProxyAiClient = ProxyAiClient(),
    private val fallback: MinimalInterventionAgent = LocalMinimalInterventionAgent.instance
) : MinimalInterventionAgent {

    override fun decideIntervention(input: InterventionDecisionInput): InterventionDecisionOutput = runBlocking {
        try {
            val response = client.chatCompletion(
                messages = listOf(
                    ChatMessage(
                        "system",
                        "You are UnDrift's minimal intervention agent. Given the context (package name, context assessment, focus mode, time spent), decide if we should intervene. " +
                                "Return strictly valid JSON with exactly keys: 'shouldIntervene' (boolean), 'level' ('NONE', 'SOFT_NUDGE', 'STRICT_OVERLAY'), " +
                                "'reason' (string or null), and 'cooldownActive' (boolean)."
                    ),
                    ChatMessage("user", input.toPrompt())
                ),
                temperature = 0.2
            )
            parseOutput(response) ?: fallback.decideIntervention(input)
        } catch (e: Exception) {
            runCatching { Log.w(TAG, "Proxy intervention decision failed; using local rules", e) }
            fallback.decideIntervention(input)
        }
    }

    private fun parseOutput(raw: String): InterventionDecisionOutput? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null

        return runCatching {
            val json = JSONObject(raw.substring(start, end + 1))
            val shouldIntervene = json.getBoolean("shouldIntervene")
            val level = InterventionLevel.valueOf(json.getString("level"))
            val reason = if (json.isNull("reason")) null else json.optString("reason").takeIf { it.isNotBlank() }
            val cooldownActive = json.optBoolean("cooldownActive", false)
            InterventionDecisionOutput(shouldIntervene, level, reason, cooldownActive)
        }.getOrNull()
    }

    private fun InterventionDecisionInput.toPrompt(): String = JSONObject().apply {
        put("packageName", packageName)
        put("contextAssessment", JSONObject().apply {
            put("context", contextAssessment.context.name)
            put("contextConfidence", contextAssessment.contextConfidence)
            put("interventionState", contextAssessment.intervention.state.name)
        })
        put("isFocusModeActive", isFocusModeActive)
        put("timeSpentMillis", timeSpentMillis)
        put("timeLimitMillis", timeLimitMillis ?: JSONObject.NULL)
        put("lastInterventionTimestamp", lastInterventionTimestamp)
        put("cooldownMillis", cooldownMillis)
    }.toString()

    companion object {
        private const val TAG = "ProxyMinimalInterventionAgent"
    }
}
