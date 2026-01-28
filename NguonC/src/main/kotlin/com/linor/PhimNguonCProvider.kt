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
            // Fix lỗi không hiện tập: Dùng trường 'episode' đã mapping đúng
            val epsNum = movie.episode?.filter { it.isDigit() }?.toIntOrNull()
            
            // Fix lỗi không hiện Sub/Dub: Dùng trường 'language' đã mapping đúng
            val isDub = movie.language?.contains("Thuyết Minh", true) == true || movie.language?.contains("Lồng Tiếng", true) == true
            val isSub = movie.language?.contains("Vietsub", true) == true

            // Fix lỗi lúc nào cũng HD: Kiểm tra kỹ hơn
            val quality = when {
                movie.quality?.contains("4K", true) == true -> SearchQuality.UHD
                movie.quality?.contains("FHD", true) == true -> SearchQuality.HD // Cloudstream coi FHD là HD
                movie.quality?.contains("HD", true) == true -> SearchQuality.HD
                movie.quality?.contains("CAM", true) == true -> SearchQuality.Cam
                else -> null
            }

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
        episodesList.forEach { server ->
            val sName = server.serverName ?: "Nguồn C"
            server.serverData?.forEach { epData ->
                // Fix lỗi Sắp có: Lấy đúng biến linkM3u8 (đã sửa trong DataClasses)
                val link = epData.linkM3u8?.takeIf { it.isNotEmpty() } ?: epData.linkEmbed
                if (!link.isNullOrBlank()) {
                    episodes.add(newEpisode("$link@@@$sName") {
                        this.name = if (epData.name?.contains("Tập") == true) epData.name else "Tập ${epData.name}"
                        this.episode = epData.name?.filter { it.isDigit() }?.toIntOrNull()
                    })
                }
            }
        }

        val isTvSeries = movie.type == "series" || episodes.size > 1
        val tvType = if (isTvSeries) TvType.TvSeries else TvType.Movie

        val plot = movie.content?.replace(Regex("<.*?>"), "")?.trim()
        val poster = movie.posterUrl ?: movie.thumbUrl
        
        // Fix Actor: API có thể trả về List String hoặc null
        val actorsData = movie.actor?.map { 
            ActorData(actor = Actor(it.trim(), "")) 
        }

        return if (tvType == TvType.TvSeries) {
            newTvSeriesLoadResponse(movie.name ?: "", url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plot
                this.year = movie.year
                this.actors = actorsData
            }
        } else {
            val firstLink = episodes.firstOrNull()?.data ?: ""
            newMovieLoadResponse(movie.name ?: "", url, TvType.Movie, firstLink) {
                this.posterUrl = poster
                this.plot = plot
                this.year = movie.year
                this.actors = actorsData
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
