package com.fluxtv.app.models
import java.io.Serializable
data class Episode(
    val title: String = "",
    val season: Int = 0,
    val episode: Int = 0,
    val streamUrl: String = ""
) : Serializable
