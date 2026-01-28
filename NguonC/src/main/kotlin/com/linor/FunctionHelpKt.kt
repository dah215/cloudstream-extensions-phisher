package com.linor

import com.lagradost.cloudstream3.network.CloudflareKiller

object FunctionHelpKt {
    var mainUrlImage = "https://phim.nguonc.com"
    val cfKiller = CloudflareKiller()

    fun getImageUrl(url: String?): String {
        if (url == null) return ""
        return if (url.startsWith("/")) "$mainUrlImage$url" else url
    }

    fun parseTitleAndSeason(input: String?): ParsedTitle {
        val name = input ?: ""
        val seasonRegex = Regex("season\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val seasonMatch = seasonRegex.find(name)
        val season = seasonMatch?.groupValues?.get(1)?.let { "Season $it" } ?: "Season 1"
        
        val title = name.replace(Regex("season\\s*\\d+", RegexOption.IGNORE_CASE), "")
                         .replace(Regex("\\b(Movie|Remake|OVA)\\b", RegexOption.IGNORE_CASE), "")
                         .trim()
        return ParsedTitle(title, season)
    }

    suspend fun extractStreamUrl(pageUrl: String): String? {
        return pageUrl 
    }
}
