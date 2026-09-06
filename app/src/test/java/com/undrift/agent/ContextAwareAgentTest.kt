package com.undrift.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContextAwareAgentTest {

    private lateinit var agent: ContextAwareAgent

    @Before
    fun setup() {
        agent = LocalContextAwareAgent()
    }

    @Test
    fun testSafeFallbackBehavior() {
        val input = ContextAssessmentInput(
            packageName = "com.any.app",
            isBlocked = true
        )
        val result = agent.assessContext(input)
        
        // Ensure local fallback fails closed and safe
        assertEquals(UserContext.UNKNOWN, result.context)
        assertEquals(ActivityCompatibility.UNKNOWN, result.activityCompatibility)
        assertEquals(InterventionState.ELIGIBLE, result.intervention.state)
        assertEquals(true, result.blocked)
        assertFalse(result.episode.active)
    }

    @Test
    fun testTypingSuppressesIntervention() {
        val input = ContextAssessmentInput(
            packageName = "com.instagram.android",
            isBlocked = true,
            isTyping = true
        )
        val result = agent.assessContext(input)
        
        // When typing, intervention MUST be strictly suppressed to protect user flow
        assertEquals(InterventionState.SUPPRESSED, result.intervention.state)
        assertEquals(ActivityCompatibility.CONSISTENT, result.activityCompatibility)
        assertEquals(0.0, result.distractionConfidence, 0.001)
        assertTrue(result.intervention.reason.contains("typing", ignoreCase = true))
    }

    @Test
    fun testDoomScrollingTriggersIntervention() {
        val input = ContextAssessmentInput(
            packageName = "com.instagram.android",
            isBlocked = true,
            isDoomScrolling = true,
            isTyping = false
        )
        val result = agent.assessContext(input)
        
        // Doom scrolling in a blocked app should trigger an eligible intervention
        assertEquals(InterventionState.ELIGIBLE, result.intervention.state)
        assertEquals(ActivityCompatibility.INCONSISTENT, result.activityCompatibility)
        assertTrue(result.distractionConfidence >= 0.9)
        assertTrue(result.intervention.reason.contains("doom scrolling", ignoreCase = true))
    }

    @Test
    fun testIdleTriggersIntervention() {
        val input = ContextAssessmentInput(
            packageName = "com.instagram.android",
            isBlocked = true,
            isIdle = true,
            isTyping = false
        )
        val result = agent.assessContext(input)
        
        // Idle in a blocked app should trigger an eligible intervention
        assertEquals(InterventionState.ELIGIBLE, result.intervention.state)
        assertEquals(UserContext.IDLE, result.context)
        assertEquals(ActivityCompatibility.INCONSISTENT, result.activityCompatibility)
        assertTrue(result.intervention.reason.contains("idle", ignoreCase = true))
    }
}
