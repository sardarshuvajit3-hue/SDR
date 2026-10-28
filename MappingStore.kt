package com.example.mousekeymapper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One key -> one screen tap. x and y are pixel positions on the screen. */
data class Mapping(val keyCode: Int, val x: Float, val y: Float)

object MappingStore {
    private const val PREFS = "mappings"
    private const val KEY_LIST = "list"
    private const val KEY_PAUSED = "paused"

    fun load(context: Context): List<Mapping> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LIST, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Mapping(o.getInt("k"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat())
        }
    }

    fun save(context: Context, list: List<Mapping>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("k", it.keyCode).put("x", it.x.toDouble()).put("y", it.y.toDouble()))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LIST, arr.toString()).apply()
    }

    /** Adds a mapping, replacing any existing one for the same key. */
    fun add(context: Context, m: Mapping) {
        save(context, load(context).filter { it.keyCode != m.keyCode } + m)
    }

    fun remove(context: Context, keyCode: Int) {
        save(context, load(context).filter { it.keyCode != keyCode })
    }

    fun isPaused(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_PAUSED, false)

    fun setPaused(context: Context, paused: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PAUSED, paused).apply()
    }
}
