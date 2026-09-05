package com.undrift.agent

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.UUID
import com.undrift.network.ProxyAiClient
import com.undrift.network.ChatMessage

class ProxyRewardLoopAgentTest {

    @Test
    fun testProxyRewardLoopAgent_Execution() = runBlocking {
        // Creating the ProxyRewardLoopAgent which internally instantiates a FakeProxyAiClient to simulate failure.
        val agent = ProxyRewardLoopAgent(
            client = object : ProxyAiClient() {
                override suspend fun chatCompletion(
                    messages: List<ChatMessage>,
                    model: String?,
                    temperature: Double?
                ): String {
                    throw RuntimeException("Simulated network failure")
                }
            }
        )

        val input = RewardEventInput(
            eventId = UUID.randomUUID().toString(),
            event = "FOCUS_SESSION_COMPLETED",
            plannedDurationMinutes = 25,
            actualFocusDurationMinutes = 25
        )

        // Calling evaluate will attempt to use the ProxyAiClient to reach the Cloudflare API.
        // In this local unit test environment (missing API keys and Android JSONObject mocks), 
        // the network call will fail.
        // The ProxyRewardLoopAgent should gracefully catch this and fallback to LocalRewardLoopAgent.
        val result = agent.evaluate(input)

        assertNotNull(result)
        
        // Asserting that the fallback logic kicked in successfully.
        assertEquals(RewardType.SESSION_COMPLETION, result.type)
        assertEquals(RewardMagnitude.MEDIUM, result.magnitude)
        assertEquals("Focus session complete.", result.message)
        
        println("Test executed successfully. Output: $result")
    }
}
