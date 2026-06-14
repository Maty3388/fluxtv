package com.fluxtv.app.models
import java.io.Serializable
data class Serie(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val posterUrl: String = "",
    val streamUrl: String = "",
    val description: String = "",
    val rating: String = "",
    val year: String = "",
    val featured: Boolean = false,
    val episodes: List<Episode> = emptyList()
) : Serializable
