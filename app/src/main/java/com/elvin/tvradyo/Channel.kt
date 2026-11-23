package com.elvin.tvradyo

data class Channel(
    val name: String,
    val url: String,
    val group: String = "Genel",
    val logo: String? = null,
    val isRadio: Boolean = false
)
