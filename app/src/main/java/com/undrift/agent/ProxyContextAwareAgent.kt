package com.undrift.agent

import android.util.Log
import com.undrift.network.ChatMessage
import com.undrift.network.ProxyAiClient
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Uses [ProxyAiClient] to assess user context via the Cloudflare AI Proxy,
 * falling back to [LocalContextAwareAgent] when offline or on error.
 */
class ProxyContextAwareAgent(
    private val client: ProxyAiClient = ProxyAiClient(),
    private val fallback: ContextAwareAgent = LocalContextAwareAgent.instance
) : ContextAwareAgent {

    override fun assessContext(input: ContextAssessmentInput): ContextAssessmentOutput = runBlocking {
        try {
            val response = client.chatCompletion(
                messages = listOf(
                    ChatMessage(
                        "system",
                        "You are UnDrift's context awareness agent. Assess the user context given app and window information. " +
                                "Return strictly valid JSON with exactly key 'context' (IMPORTANT_TASK, CASUAL_BROWSING, " +
                                "POTENTIAL_DISTRACTION, BREAK, or UNKNOWN), 'confidence' (0.0 to 1.0), and 'explanation' (string)."
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
            val context = UserContext.valueOf(json.getString("context"))
            val confidence = json.optDouble("confidence", 0.8)
            val explanation = json.optString("explanation", "AI assessed context")
            ContextAssessmentOutput(context, confidence, explanation)
        }.getOrNull()
    }

    private fun ContextAssessmentInput.toPrompt(): String = JSONObject().apply {
        put("packageName", packageName)
        put("appCategory", appCategory ?: JSONObject.NULL)
        put("windowTitle", windowTitle ?: JSONObject.NULL)
        put("isFocusModeActive", isFocusModeActive)
        put("activeGoal", activeGoal ?: JSONObject.NULL)
    }.toString()

    companion object {
        private const val TAG = "ProxyContextAwareAgent"
    }
}
