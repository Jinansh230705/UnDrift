package com.undrift.agent

object RewardPolicy {

    fun isTrivialInteraction(eventName: String): Boolean {
        return eventName in setOf(
            "APP_OPENED",
            "BUTTON_CLICKED",
            "TIMER_STARTED",
            "DASHBOARD_VIEWED",
            "NOTIFICATION_CLICKED",
            "REMINDER_DISMISSED"
        )
    }

    fun getPointsForMagnitude(magnitude: RewardMagnitude): Int {
        return when (magnitude) {
            RewardMagnitude.HIGH -> 500
            RewardMagnitude.MEDIUM -> 300
            RewardMagnitude.LOW -> 100
        }
    }

    fun getRecoveryPoints(magnitude: RewardMagnitude): Int {
        return when (magnitude) {
            RewardMagnitude.HIGH -> 50
            RewardMagnitude.MEDIUM -> 25
            RewardMagnitude.LOW -> 10
        }
    }
}
