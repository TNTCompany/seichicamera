package com.tnt.seichicamera.data.remote.dto

import com.tnt.seichicamera.data.local.entity.BangumiEntity
import com.tnt.seichicamera.data.local.entity.SacredPointEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BangumiResponse(
    @SerialName("id") val id: Int,
    @SerialName("cn") val titleCn: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("cover") val cover: String? = null,
    @SerialName("zoom") val zoom: Float? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("litePoints") val litePoints: List<LitePoint>? = null
) {
    fun toBangumiEntity(): BangumiEntity = BangumiEntity(
        id = id,
        title = titleCn ?: title ?: "Unknown",
        coverUrl = cover?.let { "https://image.anitabi.cn/bangumi/$it" } ?: "",
        region = city,
        zoom = zoom,
        cachedAt = System.currentTimeMillis()
    )

    fun toPointEntities(): List<SacredPointEntity> =
        litePoints?.mapIndexed { index, point ->
            SacredPointEntity(
                id = "${id}_$index",
                bangumiId = id,
                name = point.name,
                latitude = point.geo?.get(1) ?: 0.0,
                longitude = point.geo?.get(0) ?: 0.0,
                imageUrls = point.image?.let { img ->
                    listOf("https://image.anitabi.cn/point/$img?plan=h360")
                } ?: emptyList(),
                originUrl = point.origin,
                ep = point.ep
            )
        } ?: emptyList()
}

@Serializable
data class LitePoint(
    @SerialName("name") val name: String? = null,
    @SerialName("geo") val geo: List<Double>? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("ep") val ep: String? = null,
    @SerialName("s") val s: String? = null,
    @SerialName("origin") val origin: String? = null
)
