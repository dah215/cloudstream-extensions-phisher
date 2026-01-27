package com.linor

import com.lagradost.cloudstream3.network.CloudflareKiller
import java.net.URI

object FunctionHelpKt {
    var mainUrlImage = "https://phim.nguonc.com"
    val cfKiller = CloudflareKiller()

    fun getImageUrl(url: String): String {
        return if (url.startsWith("/")) "$mainUrlImage$url" else url
    }

    fun parseTitleAndSeason(input: String): ParsedTitle {
        val seasonRegex = Regex("season\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val seasonMatch = seasonRegex.find(input)
        val season = seasonMatch?.groupValues?.get(1)?.let { "Season $it" } ?: "Season 1"
        
        val title = input.replace(Regex("season\\s*\\d+", RegexOption.IGNORE_CASE), "")
                         .replace(Regex("\\b(Movie|Remake|OVA)\\b", RegexOption.IGNORE_CASE), "")
                         .trim()
        return ParsedTitle(title, season)
    }

    suspend fun extractStreamUrl(pageUrl: String): String? {
        return pageUrl 
    }
}
