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
    @JsonProperty("episode_current") val episode: String? = null
)

data class MovieInfo(
    @JsonProperty("movie") val movie: MovieDetailResponse? = null,
    @JsonProperty("episodes") val episodes: List<MovieEpisodeResponse>? = null
)

data class MovieDetailResponse(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("slug") val slug: String? = null,
    @JsonProperty("content") val content: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("thumb_url") val thumbUrl: String? = null,
    @JsonProperty("poster_url") val posterUrl: String? = null,
    @JsonProperty("year") val year: Int? = null,
    @JsonProperty("casts") val casts: String? = null
)

data class MovieEpisodeResponse(
    @JsonProperty("server_name") val serverName: String? = null,
    @JsonProperty("server_data") val serverData: List<EpisodeData>? = null
)

data class EpisodeData(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("link_m3u8") val linkM3u8: String? = null,
    @JsonProperty("link_embed") val linkEmbed: String? = null
)
