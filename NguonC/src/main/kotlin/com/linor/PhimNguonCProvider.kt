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

        // DỰ PHÒNG: Nếu không tìm thấy tập nào, tạo tập ảo từ chính URL phim
        // Điều này đảm bảo nút "Xem" luôn hiện, sau đó loadLinks sẽ xử lý tiếp
        if (episodes.isEmpty()) {
             episodes.add(newEpisode("$url@@@Nguồn C") {
                 this.name = "Xem Ngay"
                 this.season = 1
                 this.episode = 1
             })
        }

        val isTvSeries = movie.type == "series" || episodes.size > 1
        val tvType = if (isTvSeries) TvType.TvSeries else TvType.Movie

        val plot = movie.content?.replace(Regex("<.*?>"), "")?.trim()
        val poster = movie.posterUrl ?: movie.thumbUrl
        
        // Xử lý Actor đa hình
        val actorList = when (val a = movie.actor) {
            is String -> a.split(",").map { it.trim() }
            is List<*> -> a.mapNotNull { it.toString() }
            else -> emptyList()
        }
        val actorsData = actorList.map { ActorData(actor = Actor(it, "")) }

        // Xử lý Category đa hình
        val tagsList = mutableListOf<String>()
        try {
            val cat = movie.category
            if (cat is List<*>) {
                cat.forEach { item ->
                    if (item is Map<*, *>) (item["name"] as? String)?.let { tagsList.add(it) }
                }
            } else if (cat is Map<*, *>) {
                cat.values.forEach { group ->
                    if (group is Map<*, *>) {
                        (group["list"] as? List<*>)?.forEach { item ->
                            if (item is Map<*, *>) (item["name"] as? String)?.let { tagsList.add(it) }
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

        // Xử lý link API (Fallback từ bước load)
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

        // CHIẾN THUẬT: Thêm cả 2 loại link để đảm bảo có cái chạy được
        
        // 1. Thêm như link trực tiếp (Direct)
        callback.invoke(
            newExtractorLink(
                name = "$server (Direct)",
                source = this.name,
                url = url
            )
        )

        // 2. Thử bóc tách (Extract) nếu là link embed
        // Hàm này sẽ tự động tìm video nếu link là trang web
        loadExtractor(url, subtitleCallback, callback)
        
        return true
    }
}
