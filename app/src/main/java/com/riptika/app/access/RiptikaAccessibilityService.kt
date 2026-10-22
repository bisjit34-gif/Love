package com.riptika.app.access

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class RiptikaAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: RiptikaAccessibilityService? = null
    }
    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    /** Returns visible text of the current screen. Only called after the user approves. */
    fun dumpScreen(): String {
        val sb = StringBuilder()
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null || sb.length > 4000) return
            val t = n.text ?: n.contentDescription
            if (!t.isNullOrBlank()) sb.append(t).append('\n')
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(rootInActiveWindow)
        return sb.toString().ifBlank { "(screen empty or unreadable)" }.take(4000)
    }
}
