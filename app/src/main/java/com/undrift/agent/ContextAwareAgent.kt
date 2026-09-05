package com.undrift.agent

enum class UserContext {
    IMPORTANT_TASK,
    CASUAL_BROWSING,
    POTENTIAL_DISTRACTION,
    BREAK,
    UNKNOWN
}

data class ContextAssessmentInput(
    val packageName: String,
    val appCategory: String? = null,
    val windowTitle: String? = null,
    val isFocusModeActive: Boolean = false,
    val activeGoal: String? = null
)

data class ContextAssessmentOutput(
    val context: UserContext,
    val confidence: Double,
    val explanation: String
)

interface ContextAwareAgent {
    fun assessContext(input: ContextAssessmentInput): ContextAssessmentOutput
}

class LocalContextAwareAgent : ContextAwareAgent {
    override fun assessContext(input: ContextAssessmentInput): ContextAssessmentOutput {
        val pkg = input.packageName.lowercase()
        
        if (pkg.contains("settings") || pkg.contains("launcher") || pkg.contains("systemui")) {
            return ContextAssessmentOutput(
                context = UserContext.UNKNOWN,
                confidence = 1.0,
                explanation = "System UI or system settings application"
            )
        }

        if (pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("docs")) {
            val title = input.windowTitle?.lowercase() ?: ""
            if (title.contains("work") || title.contains("research") || title.contains("study") || title.contains("github")) {
                return ContextAssessmentOutput(
                    context = UserContext.IMPORTANT_TASK,
                    confidence = 0.9,
                    explanation = "Browser session active on work/research related material"
                )
            }
        }

        if (pkg.contains("instagram") || pkg.contains("tiktok") || pkg.contains("twitter") || pkg.contains("reddit")) {
            return ContextAssessmentOutput(
                context = UserContext.POTENTIAL_DISTRACTION,
                confidence = 0.95,
                explanation = "Social media application active during monitoring"
            )
        }

        return ContextAssessmentOutput(
            context = UserContext.CASUAL_BROWSING,
            confidence = 0.7,
            explanation = "Standard application usage"
        )
    }

    companion object {
        val instance: LocalContextAwareAgent by lazy { LocalContextAwareAgent() }
    }
}
