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
            // Kiểm tra cả 2 trường hợp tên biến chứa danh sách tập
            val dataList = server.serverData ?: server.items
            
            dataList?.forEach { epData ->
                // Kiểm tra tất cả các trường hợp tên biến chứa link
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

        val plot = movie.content?.replace(Regex("<.*?>"), "")?.trim()
        val poster = movie.posterUrl ?: movie.thumbUrl
        
        val actorsData = movie.actor?.map { 
            ActorData(actor = Actor(it.trim(), "")) 
        }

        // Xử lý Category đa hình (Map hoặc List)
        val tagsList = mutableListOf<String>()
        try {
            val cat = movie.category
            if (cat is Map<*, *>) {
                // Trường hợp là Map (như phim bộ)
                cat.values.forEach { group ->
                    if (group is Map<*, *>) { // JSON object -> Map
                        val list = group["list"] as? List<*>
                        list?.forEach { item ->
                            if (item is Map<*, *>) {
                                (item["name"] as? String)?.let { tagsList.add(it) }
                            }
                        }
                    }
                }
            } else if (cat is List<*>) {
                // Trường hợp là List (như phim lẻ)
                cat.forEach { item ->
                    if (item is Map<*, *>) {
                        (item["name"] as? String)?.let { tagsList.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            // Bỏ qua lỗi parse category để không crash app
        }

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
