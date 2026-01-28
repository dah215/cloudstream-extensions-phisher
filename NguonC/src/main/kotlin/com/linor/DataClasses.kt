package com.linor

import com.fasterxml.jackson.annotation.JsonProperty

data class ListResponse(@JsonProperty("items") val items: List<MoviesResponse>)

data class MoviesResponse(
    @JsonProperty("name") val name: String?,
    @JsonProperty("slug") val slug: String,
    @JsonProperty("thumb_url") val thumbUrl: String?,
    @JsonProperty("poster_url") val posterUrl: String?,
    @JsonProperty("quality") val quality: String?,
    @JsonProperty("language") val language: String?,
    @JsonProperty("current_episode") val episode: String?
)

data class MovieInfo(@JsonProperty("movie") val movie: MovieDetailResponse)

data class MovieDetailResponse(
    @JsonProperty("name") val name: String,
    @JsonProperty("slug") val slug: String,
    @JsonProperty("content") val content: String,
    @JsonProperty("thumb_url") val thumbUrl: String,
    @JsonProperty("poster_url") val posterUrl: String,
    @JsonProperty("casts") val casts: String?,
    @JsonProperty("category") val category: MovieCategoryResponse,
    @JsonProperty("episodes") val episodes: List<MovieEpisodeResponse>,
    @JsonProperty("total_episodes") val episode_total: Int?,
    @JsonProperty("original_name") val origin_name: String?,
    @JsonProperty("time") val time: String?,
    @JsonProperty("vote_average") val voteAverage: Double?
)

data class MovieCategoryResponse(
    @JsonProperty("2") val category2: MovieCategoryItemResponse,
    @JsonProperty("3") val category3: MovieCategoryItemResponse,
    @JsonProperty("4") val category4: MovieCategoryItemResponse
)

data class MovieCategoryItemResponse(
    @JsonProperty("list") val list: List<MovieCategoryListResponse>
)

data class MovieCategoryListResponse(
    @JsonProperty("name") val name: String
)

data class MovieEpisodeResponse(
    @JsonProperty("server_name") val serverName: String,
    @JsonProperty("items") val serverData: List<EpisodeData>
)

data class EpisodeData(
    @JsonProperty("name") val name: String,
    @JsonProperty("slug") val slug: String,
    @JsonProperty("m3u8") val linkM3u8: String,
    @JsonProperty("embed") val linkEmbed: String
)

data class Lazy(
    val backdrop: String?,
    val content: String?,
    val cast: List<ImdbActor>?,
    val genres: List<String>?,
    val vote: Int?,
    val runtime: Int?,
    val year: Int?,
    val country: String?
)

data class ImdbActor(val name: String, val image: String)
data class ParsedTitle(val title: String, val season: String)

data class Json2Detail(
    @JsonProperty("backdrop_path") val backdropPath: String?,
    @JsonProperty("overview") val content: String?,
    @JsonProperty("vote_average") val voteAverage: Double?,
    @JsonProperty("runtime") val runtime: Int?,
    @JsonProperty("last_episode_to_air") val lastEpisodeToAir: LastEpisodeToAir?,
    @JsonProperty("genres") val genres: ArrayList<Genres>?,
    @JsonProperty("origin_country") val country: List<String>?
)

data class LastEpisodeToAir(@JsonProperty("runtime") val runtime: Int?)
data class Genres(@JsonProperty("name") val name: String?)

data class Json2Cast(@JsonProperty("cast") val cast: ArrayList<Cast>?)
data class Cast(
    @JsonProperty("name") val name: String?,
    @JsonProperty("profile_path") val profilePath: String?
)
