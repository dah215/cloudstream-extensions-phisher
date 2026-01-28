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
    @JsonProperty("current_episode") val episode: String? = null, // Sửa tên biến
    @JsonProperty("quality") val quality: String? = null,
    @JsonProperty("language") val language: String? = null // Sửa tên biến
)

data class MovieInfo(
    @JsonProperty("movie") val movie: MovieDetailResponse? = null,
    @JsonProperty("episodes") val episodes: List<MovieEpisodeResponse>? = null
)

data class MovieDetailResponse(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("content") val content: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("thumb_url") val thumbUrl: String? = null,
    @JsonProperty("poster_url") val posterUrl: String? = null,
    @JsonProperty("year") val year: Int? = null,
    @JsonProperty("chieurap") val isTheater: Boolean? = null, // Thêm để check phim chiếu rạp
    @JsonProperty("actor") val actor: List<String>? = null, // API trả về List String hoặc String
    @JsonProperty("episode_total") val episode_total: String? = null
)

data class MovieEpisodeResponse(
    @JsonProperty("server_name") val serverName: String? = null,
    @JsonProperty("items") val serverData: List<EpisodeData>? = null // QUAN TRỌNG: Sửa server_data thành items
)

data class EpisodeData(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("slug") val slug: String? = null,
    @JsonProperty("m3u8") val linkM3u8: String? = null, // QUAN TRỌNG: Sửa link_m3u8 thành m3u8
    @JsonProperty("embed") val linkEmbed: String? = null // QUAN TRỌNG: Sửa link_embed thành embed
)

data class ParsedTitle(val title: String, val season: String)
