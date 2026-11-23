package com.elvin.tvradyo

object M3uParser {
    fun parse(text: String): List<Channel> {
        val result = mutableListOf<Channel>()
        var name = "Bilinmeyen kanal"
        var group = "Genel"
        var logo: String? = null
        var radio = false
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("#EXTINF", true)) {
                name = line.substringAfterLast(',').trim().ifBlank { "Bilinmeyen kanal" }
                group = Regex("group-title=\"([^\"]*)\"", RegexOption.IGNORE_CASE).find(line)?.groupValues?.get(1) ?: "Genel"
                logo = Regex("tvg-logo=\"([^\"]*)\"", RegexOption.IGNORE_CASE).find(line)?.groupValues?.get(1)
                radio = line.contains("radio", true) || line.contains("audio", true)
            } else if (line.isNotBlank() && !line.startsWith("#") && (line.startsWith("http://") || line.startsWith("https://"))) {
                result += Channel(name, line, group, logo, radio)
                name = "Bilinmeyen kanal"; group = "Genel"; logo = null; radio = false
            }
        }
        return result
    }
}
