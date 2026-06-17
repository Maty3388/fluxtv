package com.fluxtv.app.utils
import android.content.Context
object Prefs {
    private const val NAME = "flux_prefs"
    fun saveToken(ctx: Context, t: String) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("token", t).apply()
    fun getToken(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("token", "") ?: ""
    fun saveEmail(ctx: Context, e: String) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("email", e).apply()
    fun getEmail(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("email", "") ?: ""
    fun saveSubEnd(ctx: Context, s: String) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("sub_end", s).apply()
    fun getSubEnd(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("sub_end", "") ?: ""
    fun getDeviceId(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(NAME, android.content.Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", "") ?: ""
        if (id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    fun getDeviceId(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(NAME, android.content.Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", "") ?: ""
        if (id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    fun getDaysLeft(ctx: Context): Long {
        val s = getSubEnd(ctx).ifEmpty { return -1 }
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val diff = sdf.parse(s)!!.time - System.currentTimeMillis()
            diff / (1000 * 60 * 60 * 24)
        } catch(e: Exception) { -1 }
    }
    fun saveProfileSelected(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putBoolean("profile_selected", true).apply()
    fun isProfileSelected(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean("profile_selected", false)
    fun isLoggedIn(ctx: Context) = getToken(ctx).isNotEmpty()
    fun logout(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().clear().apply()

    // Continuar viendo: guarda posicion en ms por id de contenido VOD
    fun saveProgress(ctx: Context, id: String, positionMs: Long, durationMs: Long) {
        val p = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        if (durationMs <= 0) return
        val pct = positionMs.toFloat() / durationMs.toFloat()
        val editor = p.edit()
        if (pct < 0.05f || pct > 0.95f) {
            // No guardar si recien empezo o ya casi termino
            editor.remove("progress_$id")
        } else {
            editor.putLong("progress_$id", positionMs)
        }
        editor.apply()
    }

    fun getProgress(ctx: Context, id: String): Long =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getLong("progress_$id", 0L)

    fun getAllProgressIds(ctx: Context): List<String> {
        val p = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return p.all.keys.filter { it.startsWith("progress_") }.map { it.removePrefix("progress_") }
    }
}
