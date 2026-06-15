package com.fluxtv.app.models
import java.io.Serializable
data class Channel(
    val id: String = "", val name: String = "", val category: String = "",
    val logoUrl: String = "", val streamUrl: String = "",
    val number: Int = 999, val isLive: Boolean = true,
    val headers: Map<String, String> = emptyMap(),
    val drmKeys: String = ""
) : Serializable
