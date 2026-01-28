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
            newAnimeSearchResponse(movie.name ?: "", "$mainUrl/film/${movie.slug}", TvType.TvSeries) {
                this.posterUrl = movie.thumbUrl ?: movie.posterUrl
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

        // Ép kiểu TvSeries nếu có danh sách tập để tránh lỗi "Sắp có"
        val tvType = if (movie.type == "series" || episodesList.any { (it.serverData?.size ?: 0) > 1 }) 
                        TvType.TvSeries else TvType.Movie

        val episodes = mutableListOf<Episode>()
        
        // Duyệt qua từng server và từng tập phim
        episodesList.forEach { server ->
            val sName = server.serverName ?: "Nguồn C"
            server.serverData?.forEach { epData ->
                // Lấy link m3u8 hoặc embed
                val link = epData.linkM3u8?.takeIf { it.isNotBlank() } ?: epData.linkEmbed
                if (!link.isNullOrBlank()) {
                    episodes.add(newEpisode("$link@@@$sName") {
                        this.name = epData.name
                        // Cloudstream cần season và episode để hiển thị danh sách
                        this.episode = epData.name?.filter { it.isDigit() }?.toIntOrNull()
                    })
                }
            }
        }

        val plot = movie.content?.replace(Regex("<.*?>"), "")?.trim()
        val poster = movie.posterUrl ?: movie.thumbUrl
        
        // Xử lý danh sách diễn viên an toàn để Build không lỗi
        val actorsData = movie.casts?.split(",")?.map { 
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
            // Đối với phim lẻ, vẫn truyền danh sách episodes để nút "Xem" hiện ra
            newMovieLoadResponse(movie.name ?: "", url, TvType.Movie, episodes.firstOrNull()?.data ?: "") {
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

        // Sử dụng hàm newExtractorLink tối giản nhất để vượt qua Gradle 8.13
        callback.invoke(
            newExtractorLink(
                server,
                this.name,
                url
            )
        )
        return true
    }
}
