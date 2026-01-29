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
            val dataList = server.serverData ?: server.items
            
            dataList?.forEach { epData ->
                val link = epData.linkM3u8?.takeIf { it.isNotEmpty() } 
                          ?: epData.m3u8?.takeIf { it.isNotEmpty() }
                          ?: epData.linkEmbed?.takeIf { it.isNotEmpty() }
                          ?: epData.embed
                
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

        // DỰ PHÒNG: Nếu không có tập nào, tạo tập ảo để hiện nút Xem
        if (episodes.isEmpty()) {
             episodes.add(newEpisode("$url@@@Nguồn C") {
                 this.name = "Full Movie"
                 this.season = 1
                 this.episode = 1
             })
        }

        val plot = movie.content?.replace(Regex("<.*?>"), "")?.trim()
        val poster = movie.posterUrl ?: movie.thumbUrl
        
        // FIX LỖI ACTOR: Kiểm tra kiểu dữ liệu trước khi map
        val actorsData = when (val a = movie.actor) {
            is List<*> -> a.mapNotNull { it.toString() }.map { ActorData(actor = Actor(it, "")) }
            is String -> a.split(",").map { ActorData(actor = Actor(it.trim(), "")) }
            else -> emptyList()
        }

        // FIX LỖI CATEGORY: Kiểm tra kiểu dữ liệu trước khi map
        val tagsList = mutableListOf<String>()
        try {
            val cat = movie.category
            if (cat is List<*>) {
                cat.forEach { item ->
                    if (item is Map<*, *>) {
                        (item["name"] as? String)?.let { tagsList.add(it) }
                    }
                }
            } else if (cat is Map<*, *>) {
                cat.values.forEach { group ->
                    if (group is Map<*, *>) {
                        (group["list"] as? List<*>)?.forEach { item ->
                            if (item is Map<*, *>) {
                                (item["name"] as? String)?.let { tagsList.add(it) }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { }

        return if (tvType == TvType.TvSeries) {
            newTvSeriesLoadResponse(movie.name ?: "", url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plot
                this.year = movie.year
                this.actors = actorsData
                this.tags = tagsList
            }
        } else {
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
        var url = parts.getOrNull(0) ?: return false
        val server = parts.getOrNull(1) ?: "Nguồn C"

        // Xử lý link API (Fallback)
        if (url.contains("phim.nguonc.com/api/film")) {
             try {
                 val responseText = app.get(url).text
                 val dataJson = tryParseJson<MovieInfo>(responseText)
                 val ep = dataJson?.episodes?.firstOrNull()?.serverData?.firstOrNull() 
                         ?: dataJson?.episodes?.firstOrNull()?.items?.firstOrNull()
                 
                 url = ep?.linkM3u8?.takeIf { it.isNotEmpty() } 
                       ?: ep?.m3u8?.takeIf { it.isNotEmpty() }
                       ?: ep?.linkEmbed 
                       ?: ep?.embed 
                       ?: return false
             } catch (e: Exception) {
                 return false
             }
        }

        if (url.contains(".m3u8")) {
            callback.invoke(
                newExtractorLink(
                    name = server,
                    source = this.name,
                    url = url
                )
            )
        } else {
            loadExtractor(url, subtitleCallback, callback)
        }
        
        return true
    }
}
