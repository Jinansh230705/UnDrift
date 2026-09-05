package com.undrift.agent

import com.undrift.network.ChatMessage
import com.undrift.network.ProxyAiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeProxyAiClient(private val mockResponse: String? = null, private val shouldThrow: Boolean = false) : ProxyAiClient() {
    override suspend fun chatCompletion(messages: List<ChatMessage>, model: String?, temperature: Double?): String {
        if (shouldThrow) throw Exception("Network error")
        return mockResponse ?: "{}"
    }

    override fun chatCompletionStream(messages: List<ChatMessage>, model: String?, temperature: Double?): Flow<String> {
        return emptyFlow()
    }
}

class ProxyContextAwareAgentTest {

    private fun createAgent(mockJson: String? = null, shouldThrow: Boolean = false): ProxyContextAwareAgent {
        return ProxyContextAwareAgent(
            client = FakeProxyAiClient(mockJson, shouldThrow),
            fallback = LocalContextAwareAgent()
        )
    }

    private val baseInput = ContextAssessmentInput(
        packageName = "com.test.app",
        timeSpentMillis = 10000,
        isFocusModeActive = true
    )

    private fun buildJson(
        context: String = "FOCUS",
        contextConf: Double = 0.9,
        activity: String = "typing",
        compat: String = "CONSISTENT",
        distractConf: Double = 0.1,
        blocked: Boolean = false,
        epActive: Boolean = true,
        epDur: Long = 10,
        intState: String = "NOT_ELIGIBLE",
        intThresh: Long = 60,
        intElapsed: Long = 10,
        intReason: String = "Safe"
    ): String {
        return """
        {
          "context": "$context",
          "context_confidence": $contextConf,
          "current_activity": "$activity",
          "activity_compatibility": "$compat",
          "distraction_confidence": $distractConf,
          "blocked": $blocked,
          "episode": {
            "active": $epActive,
            "started_at": 1000,
            "duration_seconds": $epDur
          },
          "intervention": {
            "state": "$intState",
            "threshold_seconds": $intThresh,
            "elapsed_seconds": $intElapsed,
            "remaining_seconds": ${intThresh - intElapsed},
            "reason": "$intReason"
          },
          "transition": null,
          "evidence": ["test evidence"]
        }
        """.trimIndent()
    }

    @Test
    fun `1 Productive activity during focus`() {
        val agent = createAgent(buildJson(compat = "CONSISTENT", intState = "NOT_ELIGIBLE"))
        val result = agent.assessContext(baseInput)
        assertEquals(ActivityCompatibility.CONSISTENT, result.activityCompatibility)
        assertEquals(InterventionState.NOT_ELIGIBLE, result.intervention.state)
    }

    @Test
    fun `2 Potentially distracting activity during focus`() {
        val agent = createAgent(buildJson(compat = "POTENTIALLY_INCONSISTENT", intState = "WAITING"))
        val result = agent.assessContext(baseInput)
        assertEquals(ActivityCompatibility.POTENTIALLY_INCONSISTENT, result.activityCompatibility)
        assertEquals(InterventionState.WAITING, result.intervention.state)
    }

    @Test
    fun `3 Strong distracting activity during focus`() {
        val agent = createAgent(buildJson(compat = "INCONSISTENT", intState = "ELIGIBLE", intElapsed = 100, intThresh = 60))
        val result = agent.assessContext(baseInput)
        assertEquals(ActivityCompatibility.INCONSISTENT, result.activityCompatibility)
        assertEquals(InterventionState.ELIGIBLE, result.intervention.state)
    }

    @Test
    fun `4 Blocked application opened before threshold`() {
        val agent = createAgent(buildJson(blocked = true, intState = "WAITING", intElapsed = 10, intThresh = 60))
        val result = agent.assessContext(baseInput.copy(isBlocked = true))
        assertEquals(true, result.blocked)
        assertEquals(InterventionState.WAITING, result.intervention.state)
    }

    @Test
    fun `5 Blocked application remaining open beyond threshold`() {
        val agent = createAgent(buildJson(blocked = true, intState = "ELIGIBLE", intElapsed = 70, intThresh = 60))
        val result = agent.assessContext(baseInput.copy(isBlocked = true))
        assertEquals(InterventionState.ELIGIBLE, result.intervention.state)
    }

    @Test
    fun `7 Legitimate break`() {
        val agent = createAgent(buildJson(context = "BREAK", compat = "CONSISTENT", intState = "NOT_ELIGIBLE"))
        val result = agent.assessContext(baseInput)
        assertEquals(UserContext.BREAK, result.context)
        assertEquals(InterventionState.NOT_ELIGIBLE, result.intervention.state)
    }

    @Test
    fun `8 Unknown context`() {
        val agent = createAgent(buildJson(context = "UNKNOWN", intState = "NOT_ELIGIBLE"))
        val result = agent.assessContext(baseInput)
        assertEquals(UserContext.UNKNOWN, result.context)
    }

    @Test
    fun `13 AI proxy timeout or 14 unavailable falls back safely`() {
        val agent = createAgent(shouldThrow = true)
        val result = agent.assessContext(baseInput)
        assertEquals(UserContext.UNKNOWN, result.context)
        assertEquals(InterventionState.NOT_ELIGIBLE, result.intervention.state)
    }

    @Test
    fun `15 Malformed AI response falls back safely`() {
        val agent = createAgent(mockJson = "{ invalid json }")
        val result = agent.assessContext(baseInput)
        assertEquals(InterventionState.NOT_ELIGIBLE, result.intervention.state)
    }

    @Test
    fun `16 Missing AI response fields falls back safely`() {
        val agent = createAgent(mockJson = """{"context": "FOCUS"}""")
        val result = agent.assessContext(baseInput)
        assertEquals(InterventionState.NOT_ELIGIBLE, result.intervention.state)
    }

    @Test
    fun `20 End-to-end nudge eligibility`() {
        // Test that MinimalInterventionAgent uses ContextAwareAgent output correctly
        val ctxAgent = createAgent(buildJson(blocked = true, intState = "ELIGIBLE", intElapsed = 70, intThresh = 60))
        val ctxResult = ctxAgent.assessContext(baseInput)

        val minAgent = LocalMinimalInterventionAgent()
        val decisionInput = InterventionDecisionInput(
            packageName = "com.test.app",
            contextAssessment = ctxResult,
            isFocusModeActive = true,
            timeSpentMillis = 70000,
            cooldownMillis = 60000,
            lastInterventionTimestamp = 0
        )
        
        val decision = minAgent.decideIntervention(decisionInput)
        assertTrue(decision.shouldIntervene)
        assertEquals(InterventionLevel.STRICT_OVERLAY, decision.level)
    }
}
