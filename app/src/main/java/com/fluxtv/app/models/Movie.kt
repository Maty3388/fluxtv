package com.fluxtv.app.models
import java.io.Serializable
data class Movie(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val streamUrl: String = "",
    val description: String = "",
    val rating: String = "",
    val year: String = "",
    val featured: Boolean = false
) : Serializable
