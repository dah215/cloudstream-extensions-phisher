package aho

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.Interceptor
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.NiceResponse

class NguonC : MainAPI() {
    override var mainUrl = "https://phim.nguonc.com"
    private var directUrl = "$mainUrl/api/films"
    override var name = "NguonC"
    override val hasMainPage = true
    override var lang = "vi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/api/films/phim-moi-cap-nhat?page=" to "Mới Cập Nhật",
        "$mainUrl/api/films/danh-sach/phim-le?page=" to "Phim Lẻ",
        "$mainUrl/api/films/danh-sach/phim-bo?page=" to "Phim Bộ",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse? {
        val reps = app.get("${request.data}$page").parsed<ResponseFilm>()
        val home = reps.items.mapNotNull { toSearchResult(it) }
        return newHomePageResponse(
            HomePageList(request.name, home),
            hasNext = true // Assuming there's always a next page, or check list size
        )
    }

    private fun toSearchResult(filmData: FilmData): SearchResponse? {
        val href = "$mainUrl/api/film/${filmData.slug}"
        val poster = filmData.posterUrl
        val title = filmData.name.toString()
        val type = filmData.currentEpisode
        val quality = filmData.quality.toString()

        return if (filmData.totalEpisodes == null || filmData.totalEpisodes != 1) {
            val episode = type?.substringAfter("Tập ")?.substringBefore("/")?.toIntOrNull()
            newAnimeSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = poster
                addSub(episode)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
                addQuality(quality)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val request = app.get("$directUrl/search?keyword=$query").parsed<ResponseFilm>()
        return request.items.mapNotNull { toSearchResult(it) }
    }

    override suspend fun load(url: String): LoadResponse? {
        val request = app.get(url).parsed<ResponseMovie>()
        val movie = request.movie

        val title = movie.name.toString()
        val poster = movie.thumbUrl
        val description = movie.description?.substringAfter("<p>")?.substringBefore("</p>")
        val year = movie.category["3"]?.list?.getOrNull(0)?.name?.toIntOrNull()
        val tags = movie.category["2"]?.list?.mapNotNull { it.name.toString() }

        return if (movie.totalEpisodes == 1) {
            val link = movie.episode.getOrNull(0)?.items?.getOrNull(0)?.embed
            newMovieLoadResponse(title, url, TvType.Movie, link) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
            }
        } else {
            val epSub = movie.episode.getOrNull(0)?.items?.mapNotNull {
                it.embed?.let { embed ->
                    Episode(
                        data = embed,
                        name = "Episode ${it.name}",
                        episode = it.name?.toIntOrNull()
                    )
                }
            } ?: emptyList()

            val episodeMap = mutableMapOf(DubStatus.Subbed to epSub)

            if (movie.episode.size == 2) {
                val epDub = movie.episode.getOrNull(1)?.items?.mapNotNull {
                    it.embed?.let { embed ->
                        Episode(
                            data = embed,
                            name = "Episode ${it.name}",
                            episode = it.name?.toIntOrNull()
                        )
                    }
                } ?: emptyList()
                episodeMap[DubStatus.Dubbed] = epDub
            }

            newAnimeLoadResponse(title, url, TvType.TvSeries) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.episodes = episodeMap
            }
        }
    }


    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor? {
        return Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .removeHeader("host") // Smali had "host", not "Host"
                .build()
            chain.proceed(newRequest)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val refer = data.substringBefore("embed.php")
        callback.invoke(
            ExtractorLink(
                source = this.name, // NguonC
                name = this.name,   // NguonC
                url = data.replace("embed.php", "get.php"),
                referer = refer,
                quality = Qualities.P1080.value, // Assuming 1080p as a default
                type = ExtractorLinkType.M3U8
            )
        )
        return true
    }
}