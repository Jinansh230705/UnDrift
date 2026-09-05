package com.undrift.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

class InterventionRepository private constructor() {

    private val records = CopyOnWriteArrayList<InterventionRecord>()
    private val _interventionsFlow = MutableStateFlow<List<InterventionRecord>>(emptyList())
    val interventionsFlow: StateFlow<List<InterventionRecord>> = _interventionsFlow.asStateFlow()

    fun addRecord(record: InterventionRecord) {
        records.add(0, record)
        trimRecords()
        _interventionsFlow.value = records.toList()
    }

    fun recordOutcome(recordId: String, response: InterventionResponse) {
        val index = records.indexOfFirst { it.id == recordId }
        if (index >= 0) {
            val existing = records[index]
            records[index] = existing.copy(response = response)
            _interventionsFlow.value = records.toList()
        }
    }

    fun getRecentRecords(limit: Int = 20): List<InterventionRecord> {
        return records.take(limit)
    }

    fun getLastInterventionRecord(): InterventionRecord? {
        return records.firstOrNull { it.output.intervene }
    }

    fun clear() {
        records.clear()
        _interventionsFlow.value = emptyList()
    }

    private fun trimRecords(maxSize: Int = 50) {
        while (records.size > maxSize) {
            records.removeAt(records.size - 1)
        }
    }

    companion object {
        val instance: InterventionRepository by lazy { InterventionRepository() }
    }
}
