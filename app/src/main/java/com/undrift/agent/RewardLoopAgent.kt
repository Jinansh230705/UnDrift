package com.undrift.agent

import kotlinx.coroutines.flow.StateFlow

interface RewardLoopAgent {
    fun evaluate(input: RewardEventInput): RewardOutput
    fun getRecentEvaluations(): List<RewardEvaluationRecord>
    fun clearEvaluations()
}

class LocalRewardLoopAgent(
    private val evaluator: RewardEvaluator = RewardEvaluator(),
    private val repository: RewardRepository = RewardRepository.instance
) : RewardLoopAgent {

    companion object {
        val evaluationsFlow: StateFlow<List<RewardEvaluationRecord>>
            get() = RewardRepository.instance.evaluationsFlow

        val instance: LocalRewardLoopAgent by lazy { LocalRewardLoopAgent() }
    }

    override fun evaluate(input: RewardEventInput): RewardOutput {
        if (repository.isDuplicate(input.eventId)) {
            return RewardOutput(RewardType.NONE, RewardMagnitude.LOW, null)
        }
        repository.markProcessed(input.eventId)

        val output = evaluator.evaluateEvent(input)
        val record = RewardEvaluationRecord(input = input, output = output)
        repository.addRecord(record)

        return output
    }

    override fun getRecentEvaluations(): List<RewardEvaluationRecord> {
        return repository.getRecentRecords()
    }

    override fun clearEvaluations() {
        repository.clear()
    }
}
