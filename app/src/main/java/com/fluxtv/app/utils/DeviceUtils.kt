package com.fluxtv.app.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

object DeviceUtils {
    /**
     * Determina si el dispositivo es un Android TV / TV Box (D-pad, sin touch)
     * o un celular/tablet (touch).
     */
    fun isTV(ctx: Context): Boolean {
        val uiModeManager = ctx.getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager
        val isUiModeTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasNoTouch = !ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        val hasLeanback = ctx.packageManager.hasSystemFeature("android.software.leanback")
        return isUiModeTv || hasNoTouch || hasLeanback
    }
}
