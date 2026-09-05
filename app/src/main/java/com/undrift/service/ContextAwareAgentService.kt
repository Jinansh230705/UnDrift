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
        
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error handling accessibility event", e)
        }
    }

    private var lastScanTime = 0L

    private fun scanForImportantTasks() {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < 500) return
        lastScanTime = now

        try {
            val rootNode = rootInActiveWindow ?: return
            
            var isImportant = false
            val nodesToRecycle = mutableListOf<AccessibilityNodeInfo>()
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(rootNode)
            nodesToRecycle.add(rootNode)
            
            var nodesChecked = 0
            while (queue.isNotEmpty() && nodesChecked < 60) {
                val node = queue.removeFirst()
                nodesChecked++
                
                try {
                    val className = node.className?.toString() ?: ""
                    val viewId = node.viewIdResourceName ?: ""
                    val text = node.text?.toString()?.lowercase() ?: ""
                    
                    // Heuristics for Important Tasks
                    if (className.contains("EditText")) {
                        isImportant = true
                        break
                    }
                    if (viewId.contains("message_input") || viewId.contains("compose") || viewId.contains("reply")) {
                        isImportant = true
                        break
                    }
                    if (text.contains("type a message") || text.contains("write a comment")) {
                        isImportant = true
                        break
                    }

                    for (i in 0 until node.childCount) {
                        node.getChild(i)?.let { child ->
                            queue.add(child)
                            nodesToRecycle.add(child)
                        }
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Error reading node info: ${e.message}")
                }
            }
            
            // Clean up and recycle all allocated node infos
            for (nodeToRecycle in nodesToRecycle) {
                runCatching { nodeToRecycle.recycle() }
            }

            if (isImportant) {
                _currentContext.value = UserContext.IMPORTANT_TASK
            } else {
                if (_currentContext.value == UserContext.IMPORTANT_TASK) {
                    _currentContext.value = UserContext.UNKNOWN
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error scanning for important tasks", e)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "ContextAwareAgentService interrupted")
    }
}
