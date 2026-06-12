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
}
