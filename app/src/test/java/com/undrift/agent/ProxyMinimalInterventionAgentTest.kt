package com.undrift.agent

import com.undrift.network.ChatMessage
import com.undrift.network.ProxyAiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinFakeProxyAiClient(
    private val mockResponse: String? = null,
    private val shouldThrow: Boolean = false
) : ProxyAiClient() {
    var lastMessages: List<ChatMessage>? = null

    override suspend fun chatCompletion(messages: List<ChatMessage>, model: String?, temperature: Double?): String {
        lastMessages = messages
        if (shouldThrow) throw Exception("Simulated network failure")
        return mockResponse ?: "{}"
    }

    override fun chatCompletionStream(messages: List<ChatMessage>, model: String?, temperature: Double?): Flow<String> {
        return emptyFlow()
    }
}

class ProxyMinimalInterventionAgentTest {

    private fun mockContext(state: InterventionState): ContextAssessmentOutput {
        return ContextAssessmentOutput(
            context = UserContext.FOCUS,
            contextConfidence = 0.95,
            currentActivity = "Instagram",
            activityCompatibility = ActivityCompatibility.INCONSISTENT,
            distractionConfidence = 0.9,
            blocked = true,
            episode = EpisodeInfo(true, null, 120),
            intervention = InterventionInfo(state, 60, 120, 0, "Behavior inconsistent with focus"),
            transition = null,
            evidence = listOf("Scrolling feed")
        )
    }

    private val baseInput = InterventionDecisionInput(
        packageName = "com.instagram.android",
        contextAssessment = mockContext(InterventionState.ELIGIBLE),
        isFocusModeActive = true,
        timeSpentMillis = 120_000L,
        declaredTask = "Study for exam",
        recentInterventionsCount = 1,
        lastInterventionTimestamp = System.currentTimeMillis() - 600_000L
    )

    @Test
    fun testLevel0NoIntervention() {
        val json = """
            {
              "intervene": false,
              "level": 0,
              "message": null,
              "reason": "Silence is more helpful right now.",
              "confidence": 0.85,
              "cooldown_minutes": 0
            }
        """.trimIndent()

        val agent = ProxyMinimalInterventionAgent(
            client = MinFakeProxyAiClient(json),
            fallback = LocalMinimalInterventionAgent()
        )

        val output = agent.decideIntervention(baseInput)
        assertFalse(output.shouldIntervene)
        assertEquals(InterventionLevel.NONE, output.level)
        assertNull(output.message)
        assertEquals("Silence is more helpful right now.", output.reason)
        assertEquals(0.85, output.confidence, 0.001)
        assertEquals(0, output.cooldownMinutes)
    }

    @Test
    fun testLevel1Awareness() {
        val json = """
            {
              "intervene": true,
              "level": 1,
              "message": "Looks like you've been here for a bit.",
              "reason": "Gentle nudge to raise awareness.",
              "confidence": 0.9,
              "cooldown_minutes": 15
            }
        """.trimIndent()

        val agent = ProxyMinimalInterventionAgent(
            client = MinFakeProxyAiClient(json),
            fallback = LocalMinimalInterventionAgent()
        )

        val output = agent.decideIntervention(baseInput)
        assertTrue(output.shouldIntervene)
        assertEquals(InterventionLevel.AWARENESS, output.level)
        assertEquals("Looks like you've been here for a bit.", output.message)
        assertEquals(15, output.cooldownMinutes)
    }

    @Test
    fun testLevel2Reflection() {
        val json = """
            {
              "intervene": true,
              "level": 2,
              "message": "Want to get back to what you were working on?",
              "reason": "Encourage reflection after persistence.",
              "confidence": 0.92,
              "cooldown_minutes": 20
            }
        """.trimIndent()

        val agent = ProxyMinimalInterventionAgent(
            client = MinFakeProxyAiClient(json),
            fallback = LocalMinimalInterventionAgent()
        )

        val output = agent.decideIntervention(baseInput)
        assertTrue(output.shouldIntervene)
        assertEquals(InterventionLevel.REFLECTION, output.level)
        assertEquals("Want to get back to what you were working on?", output.message)
        assertEquals(20, output.cooldownMinutes)
    }

    @Test
    fun testLevel3ReturnToFocus() {
        val json = """
            {
              "intervene": true,
              "level": 3,
              "message": "Your focus session is still running. Want to get back to it?",
              "reason": "Strong context evidence with active session.",
              "confidence": 0.96,
              "cooldown_minutes": 30
            }
        """.trimIndent()

        val agent = ProxyMinimalInterventionAgent(
            client = MinFakeProxyAiClient(json),
            fallback = LocalMinimalInterventionAgent()
        )

        val output = agent.decideIntervention(baseInput)
        assertTrue(output.shouldIntervene)
        assertEquals(InterventionLevel.RETURN_TO_FOCUS, output.level)
        assertEquals("Your focus session is still running. Want to get back to it?", output.message)
        assertEquals(30, output.cooldownMinutes)
    }

    @Test
    fun testFallbackOnNetworkFailure() {
        val agent = ProxyMinimalInterventionAgent(
            client = MinFakeProxyAiClient(shouldThrow = true),
            fallback = LocalMinimalInterventionAgent()
        )

        val output = agent.decideIntervention(baseInput.copy(lastInterventionTimestamp = 0L))
        assertTrue(output.shouldIntervene)
        assertEquals(InterventionLevel.RETURN_TO_FOCUS, output.level)
        assertNotNull(output.message)
    }

    @Test
    fun testFallbackOnMalformedJson() {
        val agent = ProxyMinimalInterventionAgent(
            client = MinFakeProxyAiClient("Invalid non-json response"),
            fallback = LocalMinimalInterventionAgent()
        )

        val output = agent.decideIntervention(baseInput.copy(lastInterventionTimestamp = 0L))
        assertTrue(output.shouldIntervene)
        assertEquals(InterventionLevel.RETURN_TO_FOCUS, output.level)
    }
}
