package com.undrift.agent

import java.util.UUID

enum class InterventionLevel(val value: Int) {
    NONE(0),
    AWARENESS(1),
    REFLECTION(2),
    RETURN_TO_FOCUS(3);

    companion object {
        fun fromInt(value: Int): InterventionLevel =
            entries.find { it.value == value } ?: NONE
    }
}

enum class ActivityCompatibility {
    CONSISTENT,
    INCONSISTENT,
    NEUTRAL,
    UNKNOWN
}

enum class InterventionResponse {
    IGNORED,
    ACKNOWLEDGED,
    RETURNED_TO_FOCUS,
    DISMISSED
}

data class MinimalInterventionInput(
    val context: String = "UNKNOWN",
    val contextConfidence: Double = 0.5,
    val currentActivity: String,
    val activityCompatibility: ActivityCompatibility = ActivityCompatibility.UNKNOWN,
    val sessionDurationMinutes: Int = 0,
    val focusSessionActive: Boolean = false,
    val declaredTask: String? = null,
    val recentInterventions: Int = 0,
    val minutesSinceLastIntervention: Int = Int.MAX_VALUE,
    val previousInterventionResponse: InterventionResponse? = null,
    val isBreak: Boolean = false,
    val userSchedule: String? = null
)

data class MinimalInterventionOutput(
    val intervene: Boolean,
    val level: Int,
    val message: String?,
    val reason: String,
    val confidence: Double,
    val cooldownMinutes: Int
) {
    val interventionLevel: InterventionLevel
        get() = InterventionLevel.fromInt(level)

    companion object {
        fun noIntervention(
            reason: String = "Insufficient evidence that intervention would be helpful.",
            confidence: Double = 0.8,
            cooldownMinutes: Int = 0
        ) = MinimalInterventionOutput(
            intervene = false,
            level = 0,
            message = null,
            reason = reason,
            confidence = confidence.coerceIn(0.0, 1.0),
            cooldownMinutes = cooldownMinutes.coerceAtLeast(0)
        )
    }
}

data class InterventionRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val input: MinimalInterventionInput,
    val output: MinimalInterventionOutput,
    var response: InterventionResponse? = null
)

interface MinimalInterventionAgent {
    fun decideIntervention(input: MinimalInterventionInput): MinimalInterventionOutput
    fun recordOutcome(recordId: String, response: InterventionResponse) {}
    fun getRecentInterventions(): List<InterventionRecord> = emptyList()
    fun clearHistory() {}
}

class LocalMinimalInterventionAgent(
    private val evaluator: MinimalInterventionEvaluator = MinimalInterventionEvaluator(),
    private val repository: InterventionRepository = InterventionRepository.instance
) : MinimalInterventionAgent {

    override fun decideIntervention(input: MinimalInterventionInput): MinimalInterventionOutput {
        val output = evaluator.evaluate(input)
        val record = InterventionRecord(input = input, output = output)
        repository.addRecord(record)
        return output
    }

    override fun recordOutcome(recordId: String, response: InterventionResponse) {
        repository.recordOutcome(recordId, response)
    }

    override fun getRecentInterventions(): List<InterventionRecord> {
        return repository.getRecentRecords()
    }

    override fun clearHistory() {
        repository.clear()
    }

    companion object {
        val instance: LocalMinimalInterventionAgent by lazy { LocalMinimalInterventionAgent() }
    }
}
