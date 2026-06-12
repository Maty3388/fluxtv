package com.fluxtv.app.models
data class AppVersion(val version: String, val apkUrl: String, val forceUpdate: Boolean = false, val changelog: String = "")
