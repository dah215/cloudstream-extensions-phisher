package com.linor

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.Actor

object ImdbPro {
    private const val baseLink = "https://api.themoviedb.org/3"
    private const val apiKey = "6c8897d5cf5f914b3244cbe9e0f59448"
    private val imdbCache = mutableMapOf<String, Lazy>()

    suspend fun readLink(
        title: String,
        type: String,
        sea: String,
        year: Int?
    ): Lazy? {
        val cacheKey = "$type:$title:$sea"
        if (imdbCache.containsKey(cacheKey)) return imdbCache[cacheKey]

        val searchUrl = "$baseLink/search/$type?query=$title&api_key=$apiKey&year=$year"
        val searchResponse = app.get(searchUrl).text
        
        // Lấy ID đầu tiên
        val id = Regex("\"id\":\\s*(\\d+)").find(searchResponse)?.groupValues?.get(1) ?: return null

        val detailUrl = "$baseLink/$type/$id?api_key=$apiKey&language=vi-VN"
        val castUrl = "$baseLink/$type/$id/credits?api_key=$apiKey"

        val detailResponse = app.get(detailUrl).text
        val castResponse = app.get(castUrl).text

        val detail = tryParseJson<Json2Detail>(detailResponse)
        val castData = tryParseJson<Json2Cast>(castResponse)

        val actors = castData?.cast?.map { 
            ImdbActor(it.name ?: "", getImageImdb(it.profilePath ?: ""))
        }

        val genres = detail?.genres?.map { it.name ?: "" }
        val vote = (detail?.voteAverage?.times(10))?.toInt()
        val runtime = detail?.runtime ?: detail?.lastEpisodeToAir?.runtime
        val country = detail?.country?.firstOrNull()

        val result = Lazy(
            getImageImdb(detail?.backdropPath ?: ""),
            detail?.content,
            actors,
            genres,
            vote,
            runtime,
            year,
            country
        )

        imdbCache[cacheKey] = result
        return result
    }

    private fun getImageImdb(link: String): String {
        return if (link.startsWith("/")) "https://image.tmdb.org/t/p/w500$link" else link
    }
}
