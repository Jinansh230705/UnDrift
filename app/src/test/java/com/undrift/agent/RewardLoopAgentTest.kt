package com.undrift.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

class RewardLoopAgentTest {

    private lateinit var agent: RewardLoopAgent

    @Before
    fun setup() {
        agent = LocalRewardLoopAgent()
    }

    @Test
    fun testSessionCompletion_MeaningfulSession() {
        val input = RewardEventInput(
            eventId = UUID.randomUUID().toString(),
            event = "FOCUS_SESSION_COMPLETED",
            plannedDurationMinutes = 25,
            actualFocusDurationMinutes = 25
        )
        val result = agent.evaluate(input)
        assertEquals(RewardType.SESSION_COMPLETION, result.type)
        assertEquals(RewardMagnitude.MEDIUM, result.magnitude)
        assertEquals("Focus session complete.", result.message)
    }

    @Test
    fun testSessionCompletion_PartialButMeaningfulProgress() {
        val input = RewardEventInput(
            eventId = UUID.randomUUID().toString(),
            event = "FOCUS_SESSION_COMPLETED",
            plannedDurationMinutes = 25,
            actualFocusDurationMinutes = 10
        )
        val result = agent.evaluate(input)
        assertEquals(RewardType.PROGRESS, result.type)
        assertEquals(RewardMagnitude.LOW, result.magnitude)
        assertEquals("Good effort. Every bit of focus counts.", result.message)
    }

    @Test
    fun testSessionCompletion_NoMeaningfulProgress() {
        val input = RewardEventInput(
            eventId = UUID.randomUUID().toString(),
            event = "FOCUS_SESSION_COMPLETED",
            plannedDurationMinutes = 25,
            actualFocusDurationMinutes = 2
        )
        val result = agent.evaluate(input)
        assertEquals(RewardType.NONE, result.type)
        assertEquals(RewardMagnitude.LOW, result.magnitude)
        assertNull(result.message)
    }

    @Test
    fun testRecovery() {
        val input = RewardEventInput(
            eventId = UUID.randomUUID().toString(),
            event = "DISTRACTION_RECOVERED"
        )
        val result = agent.evaluate(input)
        assertEquals(RewardType.RECOVERY, result.type)
        assertEquals(RewardMagnitude.MEDIUM, result.magnitude)
        assertEquals("Nice recovery. You got back to your task.", result.message)
    }

    @Test
    fun testMilestone() {
        val input = RewardEventInput(
            eventId = UUID.randomUUID().toString(),
            event = "FOCUS_SESSION_COMPLETED",
            plannedDurationMinutes = 20,
            actualFocusDurationMinutes = 20,
            dailyFocusMinutes = 125, // Over 120 threshold
            previousRewardType = null
        )
        val result = agent.evaluate(input)
        assertEquals(RewardType.MILESTONE, result.type)
        assertEquals(RewardMagnitude.HIGH, result.magnitude)
        assertEquals("You've reached 2 hours of focus today.", result.message)
    }

    @Test
    fun testConsistency() {
        val input = RewardEventInput(
            eventId = UUID.randomUUID().toString(),
            event = "FOCUS_SESSION_COMPLETED",
            plannedDurationMinutes = 25,
            actualFocusDurationMinutes = 25,
            currentStreak = 3,
            previousStreak = 2
        )
        val result = agent.evaluate(input)
        assertEquals(RewardType.CONSISTENCY, result.type)
        assertEquals(RewardMagnitude.HIGH, result.magnitude)
    }

    @Test
    fun testTrivialInteraction() {
        val input = RewardEventInput(
            eventId = UUID.randomUUID().toString(),
            event = "APP_OPENED"
        )
        val result = agent.evaluate(input)
        assertEquals(RewardType.NONE, result.type)
        assertNull(result.message)
    }

    @Test
    fun testBreakOrUnknownEvent() {
        val input = RewardEventInput(
            eventId = UUID.randomUUID().toString(),
            event = "USER_ON_BREAK"
        )
        val result = agent.evaluate(input)
        assertEquals(RewardType.NONE, result.type)
        assertNull(result.message)
    }

    @Test
    fun testDuplicateEvent() {
        val eventId = UUID.randomUUID().toString()
        val input = RewardEventInput(
            eventId = eventId,
            event = "FOCUS_SESSION_COMPLETED",
            plannedDurationMinutes = 25,
            actualFocusDurationMinutes = 25
        )
        // First evaluation should succeed
        val firstResult = agent.evaluate(input)
        assertEquals(RewardType.SESSION_COMPLETION, firstResult.type)
        
        // Second evaluation with same eventId should be ignored (duplicate)
        val secondResult = agent.evaluate(input)
        assertEquals(RewardType.NONE, secondResult.type)
    }

    @Test
    fun testEvaluationLogging() {
        agent.clearEvaluations()
        val input = RewardEventInput(
            eventId = UUID.randomUUID().toString(),
            event = "DISTRACTION_RECOVERED"
        )
        agent.evaluate(input)
        val history = agent.getRecentEvaluations()
        assertEquals(1, history.size)
        assertEquals(RewardType.RECOVERY, history[0].output.type)

        agent.clearEvaluations()
        assertEquals(0, agent.getRecentEvaluations().size)
    }
}
