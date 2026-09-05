package com.undrift.agent

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MinimalInterventionAgentTest {

    private lateinit var agent: MinimalInterventionAgent
    private lateinit var evaluator: MinimalInterventionEvaluator

    @Before
    fun setup() {
        InterventionRepository.instance.clear()
        evaluator = MinimalInterventionEvaluator()
        agent = LocalMinimalInterventionAgent(evaluator)
    }

    @Test
    fun testBriefActivityProducesNoIntervention() {
        // Situation A from spec: User opens Instagram for 20 seconds (0-1 min).
        val input = MinimalInterventionInput(
            context = "FOCUSED",
            contextConfidence = 0.90,
            currentActivity = "com.instagram.android",
            activityCompatibility = ActivityCompatibility.INCONSISTENT,
            sessionDurationMinutes = 1,
            focusSessionActive = true,
            declaredTask = "Study for exam"
        )

        val output = agent.decideIntervention(input)
        assertFalse("Brief activity must not trigger intervention", output.intervene)
        assertEquals(0, output.level)
        assertEquals(InterventionLevel.NONE, output.interventionLevel)
        assertNull("Message must be null when intervene is false", output.message)
    }

    @Test
    fun testBreakProducesNoIntervention() {
        // Break must never be treated as failure or interrupted with productivity prompts
        val input = MinimalInterventionInput(
            context = "BREAK",
            contextConfidence = 0.95,
            currentActivity = "com.instagram.android",
            activityCompatibility = ActivityCompatibility.INCONSISTENT,
            sessionDurationMinutes = 15,
            focusSessionActive = false,
            isBreak = true
        )

        val output = agent.decideIntervention(input)
        assertFalse(output.intervene)
        assertEquals(0, output.level)
        assertNull(output.message)
    }

    @Test
    fun testTaskConsistentActivityProducesNoIntervention() {
        // E.g., Chrome or YouTube used for study/research
        val input = MinimalInterventionInput(
            context = "FOCUSED",
            contextConfidence = 0.90,
            currentActivity = "com.google.android.youtube",
            activityCompatibility = ActivityCompatibility.CONSISTENT,
            sessionDurationMinutes = 20,
            focusSessionActive = true,
            declaredTask = "Physics Lecture"
        )

        val output = agent.decideIntervention(input)
        assertFalse(output.intervene)
        assertEquals(0, output.level)
        assertNull(output.message)
    }

    @Test
    fun testLowContextConfidenceSuppressesIntervention() {
        // Spec: If context_confidence = 0.35, intervention threshold should be higher / prefer silence
        val input = MinimalInterventionInput(
            context = "POTENTIAL_DISTRACTION",
            contextConfidence = 0.40,
            currentActivity = "com.twitter.android",
            activityCompatibility = ActivityCompatibility.INCONSISTENT,
            sessionDurationMinutes = 8,
            focusSessionActive = true
        )

        val output = agent.decideIntervention(input)
        assertFalse("Uncertain context confidence must prevent intervention", output.intervene)
        assertEquals(0, output.level)
        assertNull(output.message)
    }

    @Test
    fun testActiveCooldownSuppressesIntervention() {
        // User was notified 5 minutes ago; minimum cooldown is at least 15 minutes
        val input = MinimalInterventionInput(
            context = "FOCUSED",
            contextConfidence = 0.90,
            currentActivity = "com.instagram.android",
            activityCompatibility = ActivityCompatibility.INCONSISTENT,
            sessionDurationMinutes = 8,
            focusSessionActive = true,
            recentInterventions = 1,
            minutesSinceLastIntervention = 5
        )

        val output = agent.decideIntervention(input)
        assertFalse("Active cooldown must suppress notification to prevent spam", output.intervene)
        assertEquals(0, output.level)
        assertNull(output.message)
        assertTrue(output.cooldownMinutes > 0)
    }

    @Test
    fun testAwarenessNudgeForInitialInconsistentActivity() {
        // Situation B: Initial nudge after moderate duration (e.g. 3 minutes)
        val input = MinimalInterventionInput(
            context = "FOCUSED",
            contextConfidence = 0.80,
            currentActivity = "com.instagram.android",
            activityCompatibility = ActivityCompatibility.INCONSISTENT,
            sessionDurationMinutes = 3,
            focusSessionActive = true,
            recentInterventions = 0,
            minutesSinceLastIntervention = 60
        )

        val output = agent.decideIntervention(input)
        assertTrue("Sufficient persistence warrants minimal awareness intervention", output.intervene)
        assertEquals(1, output.level)
        assertEquals(InterventionLevel.AWARENESS, output.interventionLevel)
        assertNotNull(output.message)
        assertTrue(output.cooldownMinutes >= 15)
    }

    @Test
    fun testReflectionNudgeForPersistentInconsistentActivity() {
        // Situation C: User continues for significant period (e.g. 7 minutes)
        val input = MinimalInterventionInput(
            context = "FOCUSED",
            contextConfidence = 0.85,
            currentActivity = "com.instagram.android",
            activityCompatibility = ActivityCompatibility.INCONSISTENT,
            sessionDurationMinutes = 7,
            focusSessionActive = true,
            declaredTask = "Finish quarterly report",
            recentInterventions = 0,
            minutesSinceLastIntervention = 60
        )

        val output = agent.decideIntervention(input)
        assertTrue(output.intervene)
        assertEquals(2, output.level)
        assertEquals(InterventionLevel.REFLECTION, output.interventionLevel)
        assertNotNull(output.message)
        assertTrue(output.message!!.contains("Finish quarterly report") || output.message!!.contains("working on"))
    }

    @Test
    fun testReturnToFocusForHighConfidenceFocusSession() {
        // Strong contextual evidence and justified intervention
        val input = MinimalInterventionInput(
            context = "FOCUSED",
            contextConfidence = 0.95,
            currentActivity = "com.instagram.android",
            activityCompatibility = ActivityCompatibility.INCONSISTENT,
            sessionDurationMinutes = 12,
            focusSessionActive = true,
            declaredTask = "Study for exam",
            recentInterventions = 1,
            minutesSinceLastIntervention = 30
        )

        val output = agent.decideIntervention(input)
        assertTrue(output.intervene)
        assertEquals(3, output.level)
        assertEquals(InterventionLevel.RETURN_TO_FOCUS, output.interventionLevel)
        assertNotNull(output.message)
        assertTrue(output.message!!.contains("focus session") && output.message!!.contains("still running"))
    }

    @Test
    fun testIgnoredInterventionCausesConservativeBackoff() {
        // Spec: ignored intervention -> increase cooldown -> raise threshold -> wait for stronger evidence
        val input = MinimalInterventionInput(
            context = "FOCUSED",
            contextConfidence = 0.90,
            currentActivity = "com.instagram.android",
            activityCompatibility = ActivityCompatibility.INCONSISTENT,
            sessionDurationMinutes = 4, // Below conservative 6m threshold when previously ignored
            focusSessionActive = true,
            recentInterventions = 1,
            minutesSinceLastIntervention = 35,
            previousInterventionResponse = InterventionResponse.IGNORED
        )

        val output = agent.decideIntervention(input)
        assertFalse("Repeated ignored interventions should make the agent more conservative", output.intervene)
        assertEquals(0, output.level)
    }

    @Test
    fun testForbiddenPhrasesAreNeverPresent() {
        val testInputs = listOf(
            MinimalInterventionInput(
                context = "FOCUSED",
                contextConfidence = 0.9,
                currentActivity = "com.instagram.android",
                sessionDurationMinutes = 3,
                focusSessionActive = true
            ),
            MinimalInterventionInput(
                context = "FOCUSED",
                contextConfidence = 0.9,
                currentActivity = "com.instagram.android",
                sessionDurationMinutes = 7,
                focusSessionActive = true
            ),
            MinimalInterventionInput(
                context = "FOCUSED",
                contextConfidence = 0.95,
                currentActivity = "com.instagram.android",
                sessionDurationMinutes = 14,
                focusSessionActive = true
            )
        )

        val forbidden = listOf(
            "wasting", "procrastinating", "lazy", "failed", "disappoint",
            "stop using", "get off", "need to focus", "shame", "punish"
        )

        for (input in testInputs) {
            val output = agent.decideIntervention(input)
            if (output.message != null) {
                val lower = output.message!!.lowercase()
                for (bad in forbidden) {
                    assertFalse(
                        "Message must not contain forbidden phrase '$bad': ${output.message}",
                        lower.contains(bad)
                    )
                }
            }
        }
    }

    @Test
    fun testInterventionRepositoryTracksRecordsAndOutcome() {
        val input = MinimalInterventionInput(
            context = "FOCUSED",
            contextConfidence = 0.9,
            currentActivity = "com.instagram.android",
            sessionDurationMinutes = 5,
            focusSessionActive = true
        )

        val output = agent.decideIntervention(input)
        val records = agent.getRecentInterventions()
        assertFalse(records.isEmpty())
        val record = records.first()
        assertEquals(output.level, record.output.level)

        // Record outcome
        agent.recordOutcome(record.id, InterventionResponse.RETURNED_TO_FOCUS)
        val updatedRecord = agent.getRecentInterventions().first()
        assertEquals(InterventionResponse.RETURNED_TO_FOCUS, updatedRecord.response)
    }
}
