package com.undrift.agent

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProxyMinimalInterventionAgentTest {

    private lateinit var agent: ProxyMinimalInterventionAgent

    @Before
    fun setup() {
        InterventionRepository.instance.clear()
        // Instantiating with default parameters invokes local fallback when network is unavailable
        agent = ProxyMinimalInterventionAgent()
    }

    @Test
    fun testFallbackOnBriefActivity() {
        val input = MinimalInterventionInput(
            context = "FOCUSED",
            contextConfidence = 0.90,
            currentActivity = "com.instagram.android",
            activityCompatibility = ActivityCompatibility.INCONSISTENT,
            sessionDurationMinutes = 1,
            focusSessionActive = true
        )

        val result = agent.decideIntervention(input)
        assertFalse(result.intervene)
        assertEquals(0, result.level)
        assertNull(result.message)
    }

    @Test
    fun testFallbackOnBreak() {
        val input = MinimalInterventionInput(
            context = "BREAK",
            contextConfidence = 0.95,
            currentActivity = "com.instagram.android",
            sessionDurationMinutes = 10,
            isBreak = true
        )

        val result = agent.decideIntervention(input)
        assertFalse(result.intervene)
        assertEquals(0, result.level)
    }

    @Test
    fun testFallbackOnFocusDistraction() {
        val input = MinimalInterventionInput(
            context = "FOCUSED",
            contextConfidence = 0.95,
            currentActivity = "com.instagram.android",
            activityCompatibility = ActivityCompatibility.INCONSISTENT,
            sessionDurationMinutes = 12,
            focusSessionActive = true,
            declaredTask = "Prepare slide deck",
            minutesSinceLastIntervention = 60
        )

        val result = agent.decideIntervention(input)
        assertTrue(result.intervene)
        assertTrue(result.level >= 2)
        assertNotNull(result.message)
        assertTrue(result.cooldownMinutes > 0)
    }

    @Test
    fun testInstantiationWithCustomProfileProxyClient() {
        val profile = com.undrift.data.UserProfile(
            name = "Test User",
            email = "test@example.com",
            aiApiUrl = "https://custom-proxy.example.com",
            aiApiKey = "test-key-123"
        )
        val customClient = com.undrift.network.ProxyAiClient.fromProfile(profile)
        val customAgent = ProxyMinimalInterventionAgent(client = customClient)
        val input = MinimalInterventionInput(
            context = "BREAK",
            sessionDurationMinutes = 10,
            isBreak = true,
            currentActivity = "com.instagram.android"
        )
        val result = customAgent.decideIntervention(input)
        assertFalse(result.intervene)
        assertEquals(0, result.level)
    }
}
