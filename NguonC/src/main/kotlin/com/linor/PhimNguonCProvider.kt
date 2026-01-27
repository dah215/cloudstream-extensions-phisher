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
            "$mainUrl/films/the-loai/kinh-di" to "Phim Kinh Dị",
            "$mainUrl/films/the-loai/hoat-hinh" to "Phim Anime",
            "$mainUrl/films/quoc-gia/han-quoc" to "Phim Hàn Quốc",
            "$mainUrl/films/quoc-gia/trung-quoc" to "Phim Trung Quốc",
            "$mainUrl/films/quoc-gia/viet-nam" to "Phim Việt Nam"
        )

        val result = items.map { (url, name) ->
            val fullUrl = "$url?page=$page"
            val response = app.get(fullUrl).text
            val listResponse = tryParseJson<ListResponse>(response)
            val movies = listResponse?.items ?: emptyList()
            val homePageList = parseMoviesList(movies)
            HomePageList(name, homePageList)
        }
        
        return newHomePageResponse(result)
    }

    private suspend fun parseMoviesList(items: List<MoviesResponse>): List<SearchResponse> {
        return items.mapNotNull { movie ->
            val movieUrl = "$mainUrl/film/${movie.slug}"
            val lang = movie.language?.lowercase() ?: ""
            val dub = lang.contains("thuyết minh") || lang.contains("lồng tiếng")
            val sub = lang.contains("vietsub")
            
            val poster = if (!movie.thumbUrl.isNullOrEmpty()) movie.thumbUrl else movie.posterUrl ?: return@mapNotNull null
            
            val epsRegex = Regex("\\((\\d+)/\\d+\\)")
            val eps = movie.episode?.let { 
                epsRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() 
                ?: Regex("\\d+").find(it)?.value?.toIntOrNull()
            }

            newAnimeSearchResponse(movie.name ?: "", movieUrl, TvType.TvSeries) {
                this.posterUrl = FunctionHelpKt.getImageUrl(poster)
                addDubStatus(dub, sub, eps, null)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/films/search?keyword=$query&page=1"
        val response = app.get(url).text
        val listResponse = tryParseJson<ListResponse>(response)
        val items = listResponse?.items ?: emptyList()
        return parseMoviesList(items)
    }

    override suspend fun load(url: String): LoadResponse? {
        val responseText = app.get(url).text
        val movieInfo = tryParseJson<MovieInfo>(responseText) ?: return null
        val movie = movieInfo.movie

        val type = if ((movie.episode_total ?: 0) > 1) TvType.TvSeries else TvType.Movie
        
        val (title, season) = FunctionHelpKt.parseTitleAndSeason(movie.origin_name ?: movie.name)
        val year = movie.category.category3.list.firstOrNull()?.name?.toIntOrNull() 
                  ?: movie.category.category4.list.firstOrNull()?.name?.toIntOrNull()

        val imdb = ImdbPro.readLink(title, if(type == TvType.TvSeries) "tv" else "movie", season, year)
        
        val recommendations = parseMoviesList(movie.category.category2.list.map { 
            MoviesResponse(it.name, "", "", "", "", "", "") 
        }.filter { false }) 

        val episodes = mutableListOf<Episode>()
        
        movie.episodes.sortedBy { 
            if (it.serverName.contains("Thuyết Minh") || it.serverName.contains("Lồng Tiếng")) 0 else 1 
        }.forEachIndexed { index, server ->
            server.serverData.forEachIndexed { epIndex, item ->
                val dataUrl = "${item.linkEmbed}@@@${server.serverName}"
                val epName = if (item.name.contains("Tập", true)) item.name else "Tập ${item.name}"
                
                val ep = newEpisode(dataUrl) {
                    this.name = epName
                    this.season = index + 1
                    this.episode = epIndex + 1
                }
                episodes.add(ep)
            }
        }

        val backdrop = imdb?.backdrop ?: FunctionHelpKt.getImageUrl(movie.thumbUrl)
        val poster = FunctionHelpKt.getImageUrl(movie.posterUrl)
        val description = if (Utils.countWords(movie.content) > 15) movie.content else (imdb?.content ?: movie.content)
        
        // CÁCH FIX ACTOR CHUẨN KHO PHISHER98: 
        val actorsData = imdb?.cast?.map { 
             ActorData(Actor(it.name ?: "", it.image ?: ""), role = null, voiceActor = null)
        }

        return if (type == TvType.TvSeries) {
            newTvSeriesLoadResponse(movie.name, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.backgroundPosterUrl = backdrop
                this.plot = description
                this.year = year
                this.recommendations = recommendations
                this.actors = actorsData
            }
        } else {
            newMovieLoadResponse(movie.name, url, TvType.Movie, episodes.firstOrNull()?.data ?: "") {
                this.posterUrl = poster
                this.backgroundPosterUrl = backdrop
                this.plot = description
                this.year = year
                this.recommendations = recommendations
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
        val server = parts.getOrNull(1) ?: ""

        val streamUrl = FunctionHelpKt.extractStreamUrl(url) ?: return false
        
        // FIX LỖI PRERELEASE: Không dùng type = ExtractorLinkType nữa vì bản Stable chưa có
        // Dùng bộ tham số cơ bản nhất mà kho này đang dùng cho Ophim
        callback.invoke(
            ExtractorLink(
                source = this.name,
                name = server,
                url = streamUrl,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true
            )
        )
        return true
    }
}
