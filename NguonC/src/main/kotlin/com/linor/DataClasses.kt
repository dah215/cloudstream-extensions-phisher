package com.linor

import com.fasterxml.jackson.annotation.JsonProperty

data class ListResponse(
    @JsonProperty("items") val items: List<MoviesResponse>?
)

data class MoviesResponse(
    @JsonProperty("name") val name: String?,
    @JsonProperty("slug") val slug: String?,
    @JsonProperty("thumb_url") val thumbUrl: String?,
    @JsonProperty("poster_url") val posterUrl: String?,
    @JsonProperty("episode_current") val episode: String?
)

data class MovieInfo(
    @JsonProperty("movie") val movie: MovieDetailResponse?,
    @JsonProperty("episodes") val episodes: List<MovieEpisodeResponse>?
)

data class MovieDetailResponse(
    @JsonProperty("name") val name: String?,
    @JsonProperty("origin_name") val originName: String?,
    @JsonProperty("content") val content: String?,
    @JsonProperty("type") val type: String?,
    @JsonProperty("thumb_url") val thumbUrl: String?,
    @JsonProperty("poster_url") val posterUrl: String?,
    @JsonProperty("year") val year: Int?,
    @JsonProperty("casts") val casts: String?,
    @JsonProperty("category") val category: Map<String, CategoryGroup>?
)

data class CategoryGroup(
    @JsonProperty("list") val list: List<CategoryItem>?
)

data class CategoryItem(
    @JsonProperty("name") val name: String?
)

data class MovieEpisodeResponse(
    @JsonProperty("server_name") val serverName: String?,
    @JsonProperty("server_data") val serverData: List<EpisodeData>?
)

data class EpisodeData(
    @JsonProperty("name") val name: String?,
    @JsonProperty("link_m3u8") val linkM3u8: String?,
    @JsonProperty("link_embed") val linkEmbed: String?
)

data class ParsedTitle(val title: String, val season: String)
