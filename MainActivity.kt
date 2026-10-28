package com.example.mousekeymapper

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/** Plain-code UI: enable the service, add key -> tap mappings, tap a row to delete it. */
class MainActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout
    private lateinit var pauseButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "1. Turn on \"Key Tap Mapper\" in Accessibility settings.\n" +
                "2. Connect a keyboard (USB OTG or Bluetooth).\n" +
                "3. Add a mapping: press a key, then enter where to tap.\n" +
                "Press F8 on the keyboard to pause or resume."
        })

        root.addView(button("Open Accessibility settings") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(button("Add mapping") { askForKey() })
        pauseButton = button("") {
            MappingStore.setPaused(this, !MappingStore.isPaused(this))
            refresh()
        }
        root.addView(pauseButton)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) })

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setOnClickListener { onClick() }
    }

    private fun refresh() {
        pauseButton.text = if (MappingStore.isPaused(this)) "Mapping is PAUSED - tap to resume" else "Mapping is ON - tap to pause"
        list.removeAllViews()
        val items = MappingStore.load(this)
        if (items.isEmpty()) {
            list.addView(TextView(this).apply { text = "\nNo mappings yet." })
        }
        items.forEach { m ->
            list.addView(TextView(this).apply {
                text = "${KeyEvent.keyCodeToString(m.keyCode).removePrefix("KEYCODE_")}  ->  tap (${m.x.toInt()}, ${m.y.toInt()})   [tap to delete]"
                textSize = 16f
                setPadding(0, 24, 0, 24)
                setOnClickListener {
                    MappingStore.remove(this@MainActivity, m.keyCode)
                    refresh()
                }
            })
        }
    }

    /** Step 1: wait for the person to press the key they want to map. */
    private fun askForKey() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Press a key")
            .setMessage("Press the key on your keyboard that should trigger a tap.")
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnKeyListener { d, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode != KeyEvent.KEYCODE_BACK) {
                d.dismiss()
                askForPosition(keyCode)
                true
            } else false
        }
        dialog.show()
    }

    /** Step 2: ask where on the screen the tap should land. */
    private fun askForPosition(keyCode: Int) {
        val metrics = resources.displayMetrics
        val pad = (16 * metrics.density).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
        }
        val xInput = numberField("X (0 - ${metrics.widthPixels})")
        val yInput = numberField("Y (0 - ${metrics.heightPixels})")
        box.addView(xInput)
        box.addView(yInput)

        AlertDialog.Builder(this)
            .setTitle("Tap position for ${KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")}")
            .setMessage("Pixels from the top-left corner. Screen is ${metrics.widthPixels} x ${metrics.heightPixels} in portrait; swap the sizes if the game runs in landscape.")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val x = xInput.text.toString().toFloatOrNull()
                val y = yInput.text.toString().toFloatOrNull()
                if (x == null || y == null) {
                    Toast.makeText(this, "Enter both X and Y", Toast.LENGTH_SHORT).show()
                } else {
                    MappingStore.add(this, Mapping(keyCode, x, y))
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun numberField(hintText: String) = EditText(this).apply {
        hint = hintText
        inputType = InputType.TYPE_CLASS_NUMBER
    }
}
