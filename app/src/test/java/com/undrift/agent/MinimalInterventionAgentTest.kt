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

    @Test
    fun testBypassOnImportantTask() {
        val context = ContextAssessmentOutput(
            context = UserContext.IMPORTANT_TASK,
            confidence = 0.9,
            explanation = "Work related"
        )
        val input = InterventionDecisionInput(
            packageName = "com.android.chrome",
            contextAssessment = context,
            isFocusModeActive = true,
            timeSpentMillis = 100_000L
        )
        val output = agent.decideIntervention(input)
        assertFalse(output.shouldIntervene)
        assertEquals(InterventionLevel.NONE, output.level)
    }

    @Test
    fun testInterventionOnFocusDistraction() {
        val context = ContextAssessmentOutput(
            context = UserContext.POTENTIAL_DISTRACTION,
            confidence = 0.95,
            explanation = "Social media"
        )
        val input = InterventionDecisionInput(
            packageName = "com.instagram.android",
            contextAssessment = context,
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
        val context = ContextAssessmentOutput(
            context = UserContext.POTENTIAL_DISTRACTION,
            confidence = 0.95,
            explanation = "Social media"
        )
        val recentTimestamp = System.currentTimeMillis() - 10_000L // 10s ago (cooldown 60s)
        val input = InterventionDecisionInput(
            packageName = "com.instagram.android",
            contextAssessment = context,
            isFocusModeActive = true,
            timeSpentMillis = 30_000L,
            lastInterventionTimestamp = recentTimestamp
        )
        val output = agent.decideIntervention(input)
        assertFalse(output.shouldIntervene)
        assertTrue(output.cooldownActive)
    }
}
