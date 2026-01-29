package com.linor

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ListResponse(
    @JsonProperty("items") val items: List<MoviesResponse>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MoviesResponse(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("slug") val slug: String? = null,
    @JsonProperty("thumb_url") val thumbUrl: String? = null,
    @JsonProperty("poster_url") val posterUrl: String? = null,
    @JsonProperty("episode_current") val episode_current: String? = null,
    @JsonProperty("quality") val quality: String? = null,
    @JsonProperty("lang") val lang: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovieInfo(
    @JsonProperty("movie") val movie: MovieDetailResponse? = null,
    @JsonProperty("episodes") val episodes: List<MovieEpisodeResponse>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovieDetailResponse(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("content") val content: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("thumb_url") val thumbUrl: String? = null,
    @JsonProperty("poster_url") val posterUrl: String? = null,
    @JsonProperty("year") val year: Int? = null,
    // Chấp nhận cả String và List cho Actor/Category để tránh Crash
    @JsonProperty("actor") val actor: Any? = null,
    @JsonProperty("episode_total") val episode_total: String? = null,
    @JsonProperty("category") val category: Any? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovieEpisodeResponse(
    @JsonProperty("server_name") val serverName: String? = null,
    // Bắt dính cả 2 trường hợp
    @JsonProperty("server_data") val serverData: List<EpisodeData>? = null,
    @JsonProperty("items") val items: List<EpisodeData>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodeData(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("slug") val slug: String? = null,
    // Vét cạn mọi tên biến chứa link có thể có
    @JsonProperty("link_m3u8") val linkM3u8: String? = null,
    @JsonProperty("m3u8") val m3u8: String? = null,
    @JsonProperty("link_embed") val linkEmbed: String? = null,
    @JsonProperty("embed") val embed: String? = null,
    @JsonProperty("url") val url: String? = null,
    @JsonProperty("source") val source: String? = null
)

data class ParsedTitle(val title: String, val season: String)
