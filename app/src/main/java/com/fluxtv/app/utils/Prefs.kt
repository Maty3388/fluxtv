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
