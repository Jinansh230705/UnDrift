package com.undrift.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals(InterventionState.NOT_ELIGIBLE, result.intervention.state)
        assertEquals(true, result.blocked)
        assertFalse(result.episode.active)
    }
}
