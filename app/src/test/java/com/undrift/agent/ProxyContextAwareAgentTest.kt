package com.undrift.agent

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProxyContextAwareAgentTest {

    private lateinit var agent: ProxyContextAwareAgent

    @Before
    fun setup() {
        // Instantiating with default parameters invokes local fallback when network is unavailable
        agent = ProxyContextAwareAgent()
    }

    @Test
    fun testFallbackOnImportantTask() {
        val input = ContextAssessmentInput(
            packageName = "com.android.chrome",
            windowTitle = "Researching Kotlin Coroutines on GitHub"
        )
        val result = agent.assessContext(input)
        assertEquals(UserContext.IMPORTANT_TASK, result.context)
    }

    @Test
    fun testFallbackOnSocialMediaDistraction() {
        val input = ContextAssessmentInput(
            packageName = "com.instagram.android"
        )
        val result = agent.assessContext(input)
        assertEquals(UserContext.POTENTIAL_DISTRACTION, result.context)
    }
}
