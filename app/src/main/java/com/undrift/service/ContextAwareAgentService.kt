package com.undrift.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

enum class UserContext {
    DOOM_SCROLLING,
    IMPORTANT_TASK,
    UNKNOWN
}

class ContextAwareAgentService : AccessibilityService() {

    companion object {
        private const val TAG = "ContextAwareAgent"

        @Volatile
        var instance: ContextAwareAgentService? = null
            private set
        
        // 8 seconds window: if user typed recently, they are actively composing/working
        private const val TYPING_LINGER_MS = 8000L
        // 20 seconds window without any interaction while on app => Idle
        private const val IDLE_THRESHOLD_MS = 20000L
        // Doom scrolling threshold: >= 3 scroll events within 4 seconds without typing
        private const val DOOM_SCROLL_WINDOW_MS = 4000L
        private const val DOOM_SCROLL_COUNT_THRESHOLD = 3

        private val _currentContext = MutableStateFlow(UserContext.UNKNOWN)
        val currentContext: StateFlow<UserContext> = _currentContext.asStateFlow()

        private val _isTypingFlow = MutableStateFlow(false)
        val isTypingFlow: StateFlow<Boolean> = _isTypingFlow.asStateFlow()

        private val _isDoomScrollingFlow = MutableStateFlow(false)
        val isDoomScrollingFlow: StateFlow<Boolean> = _isDoomScrollingFlow.asStateFlow()

        private val _isIdleFlow = MutableStateFlow(false)
        val isIdleFlow: StateFlow<Boolean> = _isIdleFlow.asStateFlow()

        @Volatile
        private var lastTypingTime = 0L

        @Volatile
        private var lastInteractionTime = System.currentTimeMillis()

        private val recentScrollTimes = ConcurrentLinkedQueue<Long>()

        fun isUserTyping(): Boolean {
            val now = System.currentTimeMillis()
            return (now - lastTypingTime) < TYPING_LINGER_MS
        }

        fun isUserDoomScrolling(): Boolean {
            if (isUserTyping()) return false
            val now = System.currentTimeMillis()
            pruneScrollTimes(now)
            return recentScrollTimes.size >= DOOM_SCROLL_COUNT_THRESHOLD
        }

        fun isUserIdle(): Boolean {
            if (isUserTyping()) return false
            val now = System.currentTimeMillis()
            return (now - lastInteractionTime) >= IDLE_THRESHOLD_MS
        }

        private fun pruneScrollTimes(now: Long) {
            while (true) {
                val oldest = recentScrollTimes.peek() ?: break
                if (now - oldest > DOOM_SCROLL_WINDOW_MS) {
                    recentScrollTimes.poll()
                } else {
                    break
                }
            }
        }

        fun recordTyping() {
            val now = System.currentTimeMillis()
            lastTypingTime = now
            lastInteractionTime = now
            _isTypingFlow.value = true
            _isDoomScrollingFlow.value = false
            _isIdleFlow.value = false
            _currentContext.value = UserContext.IMPORTANT_TASK
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "ContextAwareAgentService connected")
        lastInteractionTime = System.currentTimeMillis()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val now = System.currentTimeMillis()
        lastInteractionTime = now
        
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                    // Direct typing interaction
                    recordTyping()
                }

                AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                    // Check if focused view is editable
                    val source = event.source
                    if (source != null) {
                        try {
                            if (source.isEditable || (source.className?.toString()?.contains("EditText", ignoreCase = true) == true)) {
                                recordTyping()
                            }
                        } finally {
                            runCatching { source.recycle() }
                        }
                    }
                }

                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    // Reset idle on click
                    updateStateFlags(now)
                }

                AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                    if (!isUserTyping()) {
                        recentScrollTimes.add(now)
                        pruneScrollTimes(now)
                        if (recentScrollTimes.size >= DOOM_SCROLL_COUNT_THRESHOLD) {
                            _isDoomScrollingFlow.value = true
                            _currentContext.value = UserContext.DOOM_SCROLLING
                        }
                    }
                    updateStateFlags(now)
                }

                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    scanForImportantTasks(now)
                    updateStateFlags(now)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling accessibility event", e)
        }
    }

    private fun updateStateFlags(now: Long) {
        val typing = (now - lastTypingTime) < TYPING_LINGER_MS
        _isTypingFlow.value = typing

        if (typing) {
            _isDoomScrollingFlow.value = false
            _isIdleFlow.value = false
            _currentContext.value = UserContext.IMPORTANT_TASK
            return
        }

        pruneScrollTimes(now)
        val doomScrolling = recentScrollTimes.size >= DOOM_SCROLL_COUNT_THRESHOLD
        _isDoomScrollingFlow.value = doomScrolling

        val idle = (now - lastInteractionTime) >= IDLE_THRESHOLD_MS
        _isIdleFlow.value = idle

        _currentContext.value = when {
            doomScrolling -> UserContext.DOOM_SCROLLING
            else -> UserContext.UNKNOWN
        }
    }

    private var lastScanTime = 0L

    private fun scanForImportantTasks(now: Long) {
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
                    val isFocused = node.isFocused
                    val isEditable = node.isEditable
                    val className = node.className?.toString() ?: ""
                    val viewId = node.viewIdResourceName?.lowercase() ?: ""
                    val text = node.text?.toString()?.lowercase() ?: ""
                    
                    // Focused text input or active typing field
                    if (isFocused && (isEditable || className.contains("EditText", ignoreCase = true))) {
                        isImportant = true
                        break
                    }
                    if (isFocused && (viewId.contains("message_input") || viewId.contains("compose") || viewId.contains("reply"))) {
                        isImportant = true
                        break
                    }
                    if (isFocused && (text.contains("type a message") || text.contains("write a comment"))) {
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
                recordTyping()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error scanning for important tasks", e)
        }
    }

    override fun onDestroy() {
        if (instance == this) instance = null
        super.onDestroy()
    }

    override fun onInterrupt() {
        Log.d(TAG, "ContextAwareAgentService interrupted")
    }
}
