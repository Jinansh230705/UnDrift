package com.undrift.utils

import java.util.Calendar

/**
 * Provides dynamic, context-aware messages instead of hardcoded strings.
 */
object MotivationHelper {

    /**
     * Returns a greeting based on the current time of day.
     */
    fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning,"
            hour < 17 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }

    /**
     * Returns a contextual motivational message based on the user's
     * current streak and their personal best streak.
     */
    fun getStreakMessage(streakCount: Int, bestStreak: Int): String {
        return when {
            streakCount == 0 && bestStreak == 0 -> {
                listOf(
                    "Start your first streak today — every journey begins with one step",
                    "Today could be day one of something great",
                    "Ready to build a new habit? Let's begin"
                ).random()
            }
            streakCount == 0 && bestStreak > 0 -> {
                listOf(
                    "Your best was $bestStreak days — let's beat that this time",
                    "You've done $bestStreak days before, time to get back on track",
                    "Streaks reset, but your discipline doesn't. Previous best: $bestStreak days"
                ).random()
            }
            streakCount in 1..3 -> {
                listOf(
                    "Building momentum — $streakCount day${if (streakCount > 1) "s" else ""} and counting",
                    "You're getting started, keep showing up",
                    "Consistency is key — day $streakCount is in the books"
                ).random()
            }
            streakCount in 4..7 -> {
                listOf(
                    "Almost a full week — $streakCount days strong",
                    "You're on a roll, don't stop now",
                    "$streakCount days of focused work, impressive"
                ).random()
            }
            streakCount in 8..14 -> {
                listOf(
                    "Over a week straight — $streakCount days of deep focus",
                    "Your dedication is paying off, $streakCount days in",
                    "Two weeks within reach — keep pushing"
                ).random()
            }
            streakCount > bestStreak && bestStreak > 0 -> {
                "New personal record — $streakCount days and still going!"
            }
            else -> {
                listOf(
                    "$streakCount days focused — you're in the zone",
                    "Incredible discipline. $streakCount days of intentional focus",
                    "Your focus streak is on fire — $streakCount days"
                ).random()
            }
        }
    }

    fun getFocusLevel(streakCount: Int): String {
        return when (streakCount) {
            in 0..3 -> "Rookie"
            in 4..7 -> "Beginner"
            in 8..14 -> "Intermediate"
            in 15..29 -> "Advanced"
            in 30..90 -> "Expert"
            else -> "Master"
        }
    }
}

