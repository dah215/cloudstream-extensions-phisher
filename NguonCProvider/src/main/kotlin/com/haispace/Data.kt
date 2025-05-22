package com.haispace

import com.google.gson.annotations.SerializedName

data class TypeData(
    val status : String,
    val data : DataOfTypeData
)

data class DataOfTypeData(
    val items : List<Data>
)
data class DataSingle(
    val data: Data,
    val items : List<ItemMovie>
)
data class Data(
    val status : String,
    val movie : MovideDetail,
)
data class MovideDetail(
    val id : String,
    val name : String,
    val slug : String,
    val origin_name : String,
    val poster_url : String,
    val description: String,
    val thumb_url : Boolean,
    val current_episode : String,
    val total_episodes: Int,
    val quality : String,
    val casts : String,
    val category: Category,
    val episodes: List<Episodes>
)
data class ItemMovie(
    val name : String,
    val slug : String,
    val origin_name : String,
    val quality: String,
    val poster_url : String,
    val thumb_url : String,
    val total_episodes: Int,
    val current_episode: String,
    val language: String//"Vietsub + Thuyết Minh"
)
data class Episodes(
    val server_name : String,
    val items: List<Episode>
)
data class Episode(
    val name : String,
    val embed: String
)
data class Category(
   @SerializedName("1") val dinhdang: DataCategory,
   @SerializedName("2") val theloai: DataCategory,
   @SerializedName("3") val nam: DataCategory,
   @SerializedName("4") val quocgia: DataCategory

)
data class DataCategory(
    val list : List<ListItemCategory>
)
data class ListItemCategory(
    val name: String
)
data class Country(
    val name: String
)
data class Genre(
    val name: String
)

