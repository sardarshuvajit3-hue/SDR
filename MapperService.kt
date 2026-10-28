package com.example.mousekeymapper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Listens for hardware key presses (USB/Bluetooth keyboard or gamepad) and
 * performs a tap at the screen position mapped to that key.
 * F8 pauses/resumes the mapping so you can type normally.
 */
class MapperService : AccessibilityService() {

    private var mappings: Map<Int, Mapping> = emptyMap()

    override fun onServiceConnected() {
        super.onServiceConnected()
        reload()
    }

    private fun reload() {
        mappings = MappingStore.load(this).associateBy { it.keyCode }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_F8) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val paused = !MappingStore.isPaused(this)
                MappingStore.setPaused(this, paused)
                Toast.makeText(this, if (paused) "Mapping paused" else "Mapping on", Toast.LENGTH_SHORT).show()
            }
            return true
        }
        if (MappingStore.isPaused(this)) return false

        // Pick up edits made in the app without restarting the service.
        reload()
        val m = mappings[event.keyCode] ?: return false

        // Tap once per press; ignore auto-repeat while the key is held.
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            tap(m.x, m.y)
        }
        return true // consume the key so the foreground app does not also see it
    }

    private fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 40)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}
}
