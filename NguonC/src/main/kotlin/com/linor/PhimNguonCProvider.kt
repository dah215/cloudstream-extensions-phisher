@file:Suppress("DEPRECATION")
package com.linor

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.linor.shared.Utils

class PhimNguonCProvider(val plugin: PhimNguonCPlugin) : MainAPI() {
    override var mainUrl = "https://phim.nguonc.com/api"
    override var name = "Phim Nguồn C"
    override val hasMainPage = true
    override var lang = "vi"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val items = listOf(
            "$mainUrl/films/danh-sach/phim-dang-chieu" to "Phim Mới",
            "$mainUrl/films/the-loai/hanh-dong" to "Phim Hành Động",
            "$mainUrl/films/the-loai/phim-hai" to "Phim Hài Hước",
            "$mainUrl/films/the-loai/hinh-su" to "Phim Hình Sự",
            "$mainUrl/films/the-loai/co-trang" to "Phim Cổ Trang",
            "$mainUrl/films/the-loai/hoat-hinh" to "Phim Anime"
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

        val isTvSeries = (movie.episode_total?.toIntOrNull() ?: 0) > 1 || episodesList.any { (it.serverData?.size ?: 0) > 1 }
        val type = if (isTvSeries) TvType.TvSeries else TvType.Movie

        val episodes = mutableListOf<Episode>()
        episodesList.forEachIndexed { sIndex, server ->
            server.serverData?.forEachIndexed { eIndex, epData ->
                val dataUrl = "${epData.linkEmbed}@@@${server.serverName}"
                episodes.add(newEpisode(dataUrl) {
                    this.name = epData.name
                    this.season = sIndex + 1
                    this.episode = eIndex + 1
                })
            }
        }

        val plot = movie.content?.replace(Regex("<.*?>"), "") // Xóa tag HTML nếu có

        return if (type == TvType.TvSeries) {
            newTvSeriesLoadResponse(movie.name ?: "", url, TvType.TvSeries, episodes) {
                this.posterUrl = movie.posterUrl ?: movie.thumbUrl
                this.plot = plot
                this.year = movie.year
            }
        } else {
            newMovieLoadResponse(movie.name ?: "", url, TvType.Movie, episodes.firstOrNull()?.data ?: "") {
                this.posterUrl = movie.posterUrl ?: movie.thumbUrl
                this.plot = plot
                this.year = movie.year
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
