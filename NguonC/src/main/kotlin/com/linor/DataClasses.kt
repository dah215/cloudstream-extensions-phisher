package com.linor

import com.fasterxml.jackson.annotation.JsonProperty

data class ListResponse(
    @JsonProperty("items") val items: List<MoviesResponse>? = null
)

data class MoviesResponse(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("slug") val slug: String? = null,
    @JsonProperty("thumb_url") val thumbUrl: String? = null,
    @JsonProperty("poster_url") val posterUrl: String? = null,
    @JsonProperty("episode_current") val episode_current: String? = null,
    @JsonProperty("quality") val quality: String? = null,
    @JsonProperty("lang") val lang: String? = null
)

data class MovieInfo(
    @JsonProperty("movie") val movie: MovieDetailResponse? = null,
    @JsonProperty("episodes") val episodes: List<MovieEpisodeResponse>? = null
)

data class MovieDetailResponse(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("origin_name") val originName: String? = null,
    @JsonProperty("content") val content: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("thumb_url") val thumbUrl: String? = null,
    @JsonProperty("poster_url") val posterUrl: String? = null,
    @JsonProperty("year") val year: Int? = null,
    @JsonProperty("actor") val actor: List<String>? = null,
    @JsonProperty("episode_total") val episode_total: String? = null,
    // SỬA: Category là List chứ không phải Map
    @JsonProperty("category") val category: List<CategoryItem>? = null
)

data class CategoryItem(
    @JsonProperty("name") val name: String? = null
)

data class MovieEpisodeResponse(
    @JsonProperty("server_name") val serverName: String? = null,
    // SỬA QUAN TRỌNG: Dùng 'items' thay vì 'server_data'
    @JsonProperty("items") val items: List<EpisodeData>? = null
)

data class EpisodeData(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("slug") val slug: String? = null,
    // SỬA QUAN TRỌNG: Dùng 'm3u8' và 'embed' ngắn gọn
    @JsonProperty("m3u8") val linkM3u8: String? = null,
    @JsonProperty("embed") val linkEmbed: String? = null
)

data class ParsedTitle(val title: String, val season: String)
