package com.fluxtv.app.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Historial de contenido visto: canales en vivo, peliculas y series.
 * Guardado como JSON array en SharedPreferences, maximo MAX_ITEMS entradas.
 */
data class HistoryEntry(
    val id: String,
    val name: String,
    val category: String,
    val posterUrl: String,
    val streamUrl: String,
    val type: String, // "channel" | "movie" | "serie"
    val timestamp: Long
)

object WatchHistory {
    private const val NAME = "flux_prefs"
    private const val KEY = "watch_history"
    private const val MAX_ITEMS = 30

    fun add(ctx: Context, entry: HistoryEntry) {
        val p = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val arr = JSONArray(p.getString(KEY, "[]"))
        val newArr = JSONArray()

        // Filtrar entradas previas con el mismo id (para moverla al frente)
        val obj = JSONObject().apply {
            put("id", entry.id); put("name", entry.name); put("category", entry.category)
            put("posterUrl", entry.posterUrl); put("streamUrl", entry.streamUrl)
            put("type", entry.type); put("timestamp", entry.timestamp)
        }
        newArr.put(obj)

        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            if (item.optString("id") != entry.id && newArr.length() < MAX_ITEMS) {
                newArr.put(item)
            }
        }
        p.edit().putString(KEY, newArr.toString()).apply()
    }

    fun getAll(ctx: Context): List<HistoryEntry> {
        val p = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val arr = JSONArray(p.getString(KEY, "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            HistoryEntry(
                o.optString("id"), o.optString("name"), o.optString("category"),
                o.optString("posterUrl"), o.optString("streamUrl"), o.optString("type"),
                o.optLong("timestamp")
            )
        }
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
