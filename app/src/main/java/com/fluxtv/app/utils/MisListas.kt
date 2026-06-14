package com.fluxtv.app.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Listas personalizadas creadas por el usuario, guardadas localmente.
 * Estructura: { "Mi Lista 1": [ {id,name,category,posterUrl,streamUrl,type}, ... ], ... }
 */
data class ListItem(
    val id: String, val name: String, val category: String,
    val posterUrl: String, val streamUrl: String, val type: String
)

object MisListas {
    private const val NAME = "flux_prefs"
    private const val KEY = "mis_listas"

    private fun load(ctx: Context): JSONObject {
        val p = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return JSONObject(p.getString(KEY, "{}"))
    }

    private fun save(ctx: Context, obj: JSONObject) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString(KEY, obj.toString()).apply()
    }

    fun getListNames(ctx: Context): List<String> {
        val obj = load(ctx)
        return obj.keys().asSequence().toList()
    }

    fun createList(ctx: Context, name: String) {
        val obj = load(ctx)
        if (!obj.has(name)) {
            obj.put(name, JSONArray())
            save(ctx, obj)
        }
    }

    fun deleteList(ctx: Context, name: String) {
        val obj = load(ctx)
        obj.remove(name)
        save(ctx, obj)
    }

    fun getItems(ctx: Context, listName: String): List<ListItem> {
        val obj = load(ctx)
        val arr = obj.optJSONArray(listName) ?: JSONArray()
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ListItem(o.optString("id"), o.optString("name"), o.optString("category"),
                o.optString("posterUrl"), o.optString("streamUrl"), o.optString("type"))
        }
    }

    fun addItem(ctx: Context, listName: String, item: ListItem) {
        val obj = load(ctx)
        val arr = obj.optJSONArray(listName) ?: JSONArray()
        // Evitar duplicados
        for (i in 0 until arr.length()) {
            if (arr.getJSONObject(i).optString("id") == item.id) return
        }
        val o = JSONObject().apply {
            put("id", item.id); put("name", item.name); put("category", item.category)
            put("posterUrl", item.posterUrl); put("streamUrl", item.streamUrl); put("type", item.type)
        }
        arr.put(o)
        obj.put(listName, arr)
        save(ctx, obj)
    }

    fun removeItem(ctx: Context, listName: String, id: String) {
        val obj = load(ctx)
        val arr = obj.optJSONArray(listName) ?: return
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("id") != id) newArr.put(o)
        }
        obj.put(listName, newArr)
        save(ctx, obj)
    }
}
