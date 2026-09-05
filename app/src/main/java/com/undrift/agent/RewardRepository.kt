package com.undrift.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RewardRepository {

    private val processedEventIds = mutableSetOf<String>()
    private val evaluationsList = mutableListOf<RewardEvaluationRecord>()
    private val _evaluationsFlow = MutableStateFlow<List<RewardEvaluationRecord>>(emptyList())
    val evaluationsFlow: StateFlow<List<RewardEvaluationRecord>> = _evaluationsFlow.asStateFlow()

    fun isDuplicate(eventId: String): Boolean {
        return processedEventIds.contains(eventId)
    }

    fun markProcessed(eventId: String) {
        processedEventIds.add(eventId)
    }

    fun addRecord(record: RewardEvaluationRecord) {
        evaluationsList.add(0, record)
        val current = _evaluationsFlow.value.toMutableList()
        current.add(0, record)
        _evaluationsFlow.value = current.take(50)
    }

    fun getRecentRecords(): List<RewardEvaluationRecord> {
        return evaluationsList.toList()
    }

    fun clear() {
        processedEventIds.clear()
        evaluationsList.clear()
        _evaluationsFlow.value = emptyList()
    }

    companion object {
        val instance: RewardRepository by lazy { RewardRepository() }
    }
}
