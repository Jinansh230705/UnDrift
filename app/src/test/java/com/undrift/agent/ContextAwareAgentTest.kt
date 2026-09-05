package com.undrift.agent

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ContextAwareAgentTest {

    private lateinit var agent: ContextAwareAgent

    @Before
    fun setup() {
        agent = LocalContextAwareAgent()
    }

    @Test
    fun testImportantTaskDetection() {
        val input = ContextAssessmentInput(
            packageName = "com.android.chrome",
            windowTitle = "Researching Kotlin Coroutines on GitHub"
        )
        val result = agent.assessContext(input)
        assertEquals(UserContext.IMPORTANT_TASK, result.context)
    }

    @Test
    fun testSocialMediaDistraction() {
        val input = ContextAssessmentInput(
            packageName = "com.instagram.android"
        )
        val result = agent.assessContext(input)
        assertEquals(UserContext.POTENTIAL_DISTRACTION, result.context)
    }

    @Test
    fun testSystemUiApp() {
        val input = ContextAssessmentInput(
            packageName = "com.android.settings"
        )
        val result = agent.assessContext(input)
        assertEquals(UserContext.UNKNOWN, result.context)
    }
}
