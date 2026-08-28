package com.tnt.seichicamera.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BangumiSearchResponse(
    @SerialName("results") val results: Int = 0,
    @SerialName("list") val list: List<BangumiSearchItem>? = null
)

@Serializable
data class BangumiSearchItem(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("name_cn") val nameCn: String? = null,
    @SerialName("images") val images: BangumiImages? = null,
    @SerialName("air_date") val airDate: String? = null
)

@Serializable
data class BangumiImages(
    @SerialName("grid") val grid: String? = null,
    @SerialName("small") val small: String? = null,
    @SerialName("medium") val medium: String? = null
)
