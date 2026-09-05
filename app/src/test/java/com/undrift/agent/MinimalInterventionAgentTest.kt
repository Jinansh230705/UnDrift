package com.undrift.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MinimalInterventionAgentTest {

    private lateinit var agent: MinimalInterventionAgent

    @Before
    fun setup() {
        agent = LocalMinimalInterventionAgent()
    }

    private fun mockContext(state: InterventionState): ContextAssessmentOutput {
        return ContextAssessmentOutput(
            context = UserContext.UNKNOWN,
            contextConfidence = 1.0,
            currentActivity = "Testing",
            activityCompatibility = ActivityCompatibility.UNKNOWN,
            distractionConfidence = 1.0,
            blocked = false,
            episode = EpisodeInfo(true, null, 100),
            intervention = InterventionInfo(state, 60, 100, 0, "Reason"),
            transition = null,
            evidence = emptyList()
        )
    }

    @Test
    fun testBypassOnNotEligible() {
        val input = InterventionDecisionInput(
            packageName = "com.android.chrome",
            contextAssessment = mockContext(InterventionState.NOT_ELIGIBLE),
            isFocusModeActive = true,
            timeSpentMillis = 100_000L
        )
        val output = agent.decideIntervention(input)
        assertFalse(output.shouldIntervene)
        assertEquals(InterventionLevel.NONE, output.level)
    }

    @Test
    fun testInterventionOnEligibleFocus() {
        val input = InterventionDecisionInput(
            packageName = "com.instagram.android",
            contextAssessment = mockContext(InterventionState.ELIGIBLE),
            isFocusModeActive = true,
            timeSpentMillis = 30_000L,
            lastInterventionTimestamp = 0L
        )
        val output = agent.decideIntervention(input)
        assertTrue(output.shouldIntervene)
        assertEquals(InterventionLevel.STRICT_OVERLAY, output.level)
    }

    @Test
    fun testCooldownSuppression() {
        val recentTimestamp = System.currentTimeMillis() - 10_000L // 10s ago (cooldown 60s)
        val input = InterventionDecisionInput(
            packageName = "com.instagram.android",
            contextAssessment = mockContext(InterventionState.ELIGIBLE),
            isFocusModeActive = true,
            timeSpentMillis = 30_000L,
            lastInterventionTimestamp = recentTimestamp
        )
        val output = agent.decideIntervention(input)
        assertFalse(output.shouldIntervene)
        assertTrue(output.cooldownActive)
    }
}
