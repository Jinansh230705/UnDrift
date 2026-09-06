package com.undrift.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConduitRateLimiterTest {

    @Test
    fun testConduitRateLimiterEnforcesMax5RPM() {
        // Reset or fill the limiter to test its sliding window
        // Clear penalty if any
        ConduitRateLimiter.recordRateLimitPenalty(0)

        // Drain existing permits
        while (ConduitRateLimiter.tryAcquire()) {
            // consume remaining in this minute
        }

        assertEquals(0, ConduitRateLimiter.getRemainingRequests())
        assertFalse("Rate limiter should deny 6th request within window", ConduitRateLimiter.tryAcquire())
    }

    @Test
    fun testConduitRateLimiterPenalty() {
        // Apply a 10-second penalty
        ConduitRateLimiter.recordRateLimitPenalty(10)
        assertFalse("During penalty window, tryAcquire should return false", ConduitRateLimiter.tryAcquire())
        assertEquals(0, ConduitRateLimiter.getRemainingRequests())

        // Clear penalty
        ConduitRateLimiter.recordRateLimitPenalty(0)
    }
}
