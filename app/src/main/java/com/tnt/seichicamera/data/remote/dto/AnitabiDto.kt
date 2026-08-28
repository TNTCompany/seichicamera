package com.tnt.seichicamera.data.remote.dto

import com.tnt.seichicamera.data.local.entity.BangumiEntity
import com.tnt.seichicamera.data.local.entity.SacredPointEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Response from GET /bangumi/{subjectId}/lite
 * Used for metadata (title, cover, zoom, city) only.
 */
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
        coverUrl = cover?.toFullImageUrl() ?: "",
        region = city,
        zoom = zoom,
        cachedAt = System.currentTimeMillis()
    )
}

/**
 * Response item from GET /bangumi/{subjectId}/points/detail
 * Returns a JSON array of these objects directly.
 */
@Serializable
data class PointDetailItem(
    @SerialName("id") val id: String,
    @SerialName("cn") val cn: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("ep") val ep: JsonElement? = null,
    @SerialName("s") val s: Int? = null,
    @SerialName("geo") val geo: List<Double>? = null,
    @SerialName("origin") val origin: String? = null,
    @SerialName("originURL") val originURL: String? = null
) {
    fun toEntity(bangumiId: Int): SacredPointEntity = SacredPointEntity(
        id = id,
        bangumiId = bangumiId,
        name = cn ?: name,
        latitude = geo?.getOrNull(0) ?: 0.0,
        longitude = geo?.getOrNull(1) ?: 0.0,
        imageUrls = image?.let { listOf(it.toFullImageUrl()) } ?: emptyList(),
        originUrl = originURL,
        ep = ep?.let {
            when (it) {
                is JsonPrimitive -> it.content
                else -> it.toString()
            }
        }
    )
}

/**
 * Kept for backward compatibility with lite endpoint parsing.
 */
@Serializable
data class LitePoint(
    @SerialName("name") val name: String? = null,
    @SerialName("geo") val geo: List<Double>? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("ep") val ep: String? = null,
    @SerialName("s") val s: String? = null,
    @SerialName("origin") val origin: String? = null
)

/**
 * Helper: if the URL is already absolute (starts with http), use as-is.
 * Otherwise, treat it as a relative path and prepend the Anitabi image CDN.
 */
private fun String.toFullImageUrl(): String =
    if (startsWith("http://") || startsWith("https://")) this
    else "https://image.anitabi.cn/$this"
