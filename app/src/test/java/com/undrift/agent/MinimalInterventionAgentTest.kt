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
        assertEquals(InterventionLevel.RETURN_TO_FOCUS, output.level)
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
        assertTrue(output.cooldownMinutes > 0)
    }

    @Test
    fun testSituationABriefActivityNoIntervention() {
        val input = InterventionDecisionInput(
            packageName = "com.instagram.android",
            contextAssessment = mockContext(InterventionState.ELIGIBLE),
            isFocusModeActive = true,
            timeSpentMillis = 15_000L, // 15 seconds
            lastInterventionTimestamp = 0L
        )
        val output = agent.decideIntervention(input)
        assertFalse(output.shouldIntervene)
        assertEquals(InterventionLevel.NONE, output.level)
    }

    @Test
    fun testBreakContextNoIntervention() {
        val breakContext = mockContext(InterventionState.ELIGIBLE).copy(context = UserContext.BREAK)
        val input = InterventionDecisionInput(
            packageName = "com.instagram.android",
            contextAssessment = breakContext,
            isFocusModeActive = false,
            timeSpentMillis = 600_000L, // 10 mins
            lastInterventionTimestamp = 0L
        )
        val output = agent.decideIntervention(input)
        assertFalse(output.shouldIntervene)
        assertEquals(InterventionLevel.NONE, output.level)
    }

    @Test
    fun testSituationDRepeatedIgnoredInterventionsIncreasesCooldownAndRemainsConservative() {
        val input = InterventionDecisionInput(
            packageName = "com.instagram.android",
            contextAssessment = mockContext(InterventionState.ELIGIBLE),
            isFocusModeActive = true,
            timeSpentMillis = 600_000L,
            lastInterventionTimestamp = 0L,
            recentInterventionsCount = 2,
            previousInterventionResponse = "ignored"
        )
        val output = agent.decideIntervention(input)
        assertTrue(output.shouldIntervene)
        // Remains conservative (Awareness rather than escalating to level 3)
        assertEquals(InterventionLevel.AWARENESS, output.level)
        assertTrue(output.cooldownMinutes >= 20)
    }
}
