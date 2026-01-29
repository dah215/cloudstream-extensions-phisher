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
    @JsonProperty("origin_name") val originName: String? = null,
    @JsonProperty("content") val content: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("thumb_url") val thumbUrl: String? = null,
    @JsonProperty("poster_url") val posterUrl: String? = null,
    @JsonProperty("year") val year: Int? = null,
    @JsonProperty("actor") val actor: List<String>? = null,
    @JsonProperty("episode_total") val episode_total: String? = null,
    // SỬA LỖI CRASH: Đưa về List<CategoryItem> để khớp với mọi phim
    @JsonProperty("category") val category: List<CategoryItem>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CategoryItem(
    @JsonProperty("name") val name: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovieEpisodeResponse(
    @JsonProperty("server_name") val serverName: String? = null,
    @JsonProperty("server_data") val serverData: List<EpisodeData>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodeData(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("slug") val slug: String? = null,
    @JsonProperty("link_m3u8") val linkM3u8: String? = null,
    @JsonProperty("link_embed") val linkEmbed: String? = null
)

data class ParsedTitle(val title: String, val season: String)
