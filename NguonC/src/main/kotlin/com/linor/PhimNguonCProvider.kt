@file:Suppress("DEPRECATION")
package com.linor

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson

class PhimNguonCProvider(val plugin: PhimNguonCPlugin) : MainAPI() {
    override var mainUrl = "https://phim.nguonc.com/api"
    override var name = "Phim Nguồn C"
    override val hasMainPage = true
    override var lang = "vi"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val items = listOf(
            "$mainUrl/films/danh-sach/phim-moi-cap-nhat" to "Phim Mới Cập Nhật",
            "$mainUrl/films/danh-sach/phim-bo" to "Phim Bộ",
            "$mainUrl/films/danh-sach/phim-le" to "Phim Lẻ",
            "$mainUrl/films/the-loai/hanh-dong" to "Phim Hành Động",
            "$mainUrl/films/the-loai/hoat-hinh" to "Phim Hoạt Hình"
        )

        val result = items.map { (url, name) ->
            val response = app.get("$url?page=$page").text
            val movies = tryParseJson<ListResponse>(response)?.items ?: emptyList()
            HomePageList(name, parseMoviesList(movies))
        }
        return newHomePageResponse(result)
    }

    private fun parseMoviesList(items: List<MoviesResponse>): List<SearchResponse> {
        return items.mapNotNull { movie ->
            val movieUrl = "$mainUrl/film/${movie.slug}"
            val epsNum = movie.episode_current?.filter { it.isDigit() }?.toIntOrNull()
            val isDub = movie.lang?.contains("Thuyết Minh", true) == true
            val isSub = movie.lang?.contains("Vietsub", true) == true
            val quality = if (movie.quality?.contains("HD", true) == true) SearchQuality.HD else null

            newAnimeSearchResponse(movie.name ?: "", movieUrl, TvType.TvSeries) {
                this.posterUrl = movie.thumbUrl ?: movie.posterUrl
                addDubStatus(isDub, isSub, epsNum)
                this.quality = quality
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("$mainUrl/films/search?keyword=$query").text
        val items = tryParseJson<ListResponse>(response)?.items ?: emptyList()
        return parseMoviesList(items)
    }

    override suspend fun load(url: String): LoadResponse? {
        val responseText = app.get(url).text
        val data = tryParseJson<MovieInfo>(responseText) ?: return null
        val movie = data.movie ?: return null
        val episodesList = data.episodes ?: emptyList()

        val episodes = mutableListOf<Episode>()
        
        episodesList.forEachIndexed { index, server ->
            val sName = server.serverName ?: "Server ${index + 1}"
            server.serverData?.forEach { epData ->
                // Lấy link: Ưu tiên m3u8
                val link = epData.linkM3u8?.takeIf { it.isNotEmpty() } ?: epData.linkEmbed
                
                if (!link.isNullOrBlank()) {
                    val epName = epData.name ?: "Full"
                    val epNum = epName.filter { it.isDigit() }.toIntOrNull()
                    
                    episodes.add(newEpisode("$link@@@$sName") {
                        this.name = if (epName.contains("Tập", true)) epName else "Tập $epName"
                        this.season = 1
                        this.episode = epNum
                    })
                }
            }
        }

        val isTvSeries = movie.type == "series" || episodes.size > 1
        val tvType = if (isTvSeries) TvType.TvSeries else TvType.Movie

        val plot = movie.content?.replace(Regex("<.*?>"), "")?.trim()
        val poster = movie.posterUrl ?: movie.thumbUrl
        
        val actorsData = movie.actor?.map { 
            ActorData(actor = Actor(it.trim(), "")) 
        }

        val tagsList = movie.category?.mapNotNull { it.name }

        return if (tvType == TvType.TvSeries) {
            newTvSeriesLoadResponse(movie.name ?: "", url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plot
                this.year = movie.year
                this.actors = actorsData
                this.tags = tagsList
            }
        } else {
            // Lấy link đầu tiên cho phim lẻ
            val firstLink = episodes.firstOrNull()?.data ?: ""
            newMovieLoadResponse(movie.name ?: "", url, TvType.Movie, firstLink) {
                this.posterUrl = poster
                this.plot = plot
                this.year = movie.year
                this.actors = actorsData
                this.tags = tagsList
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val parts = data.split("@@@")
        val url = parts.getOrNull(0) ?: return false
        val server = parts.getOrNull(1) ?: "Nguồn C"

        callback.invoke(
            newExtractorLink(
                name = server,
                source = this.name,
                url = url
            )
        )
        return true
    }
}
