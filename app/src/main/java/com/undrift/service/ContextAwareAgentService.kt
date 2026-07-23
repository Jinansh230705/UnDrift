package com.undrift.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserContext {
    DOOM_SCROLLING,
    IMPORTANT_TASK,
    UNKNOWN
}

class ContextAwareAgentService : AccessibilityService() {

    companion object {
        private const val TAG = "ContextAwareAgent"
        
        private val _currentContext = MutableStateFlow(UserContext.UNKNOWN)
        val currentContext: StateFlow<UserContext> = _currentContext.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "ContextAwareAgentService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // Rapid scrolling without text input strongly suggests Doom Scrolling
                if (_currentContext.value != UserContext.IMPORTANT_TASK) {
                    _currentContext.value = UserContext.DOOM_SCROLLING
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Debounce or limit how often we scan the full tree
                scanForImportantTasks()
            }
        }
    }

    private fun scanForImportantTasks() {
        val rootNode = rootInActiveWindow ?: return
        
        var isImportant = false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)
        
        var nodesChecked = 0
        while (queue.isNotEmpty() && nodesChecked < 100) {
            val node = queue.removeFirst()
            nodesChecked++
            
            val className = node.className?.toString() ?: ""
            val viewId = node.viewIdResourceName ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""
            
            // Heuristics for Important Tasks
            // 1. Messaging/Emails (EditText focused)
            if (className.contains("EditText")) {
                isImportant = true
                node.recycle()
                break
            }
            // 2. Chat inputs
            if (viewId.contains("message_input") || viewId.contains("compose") || viewId.contains("reply")) {
                isImportant = true
                node.recycle()
                break
            }
            // 3. Document editing / creation
            if (text.contains("type a message") || text.contains("write a comment")) {
                isImportant = true
                node.recycle()
                break
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
            node.recycle()
        }
        
        // Clean up remaining queue
        while(queue.isNotEmpty()) {
            queue.removeFirst().recycle()
        }
        
        if (isImportant) {
            _currentContext.value = UserContext.IMPORTANT_TASK
        } else {
            // Revert to UNKNOWN if we were important but left the important screen
            if (_currentContext.value == UserContext.IMPORTANT_TASK) {
                _currentContext.value = UserContext.UNKNOWN
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "ContextAwareAgentService interrupted")
    }
}
