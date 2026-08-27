package com.tnt.seichicamera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tnt.seichicamera.domain.model.Bangumi

@Entity(tableName = "bangumi")
data class BangumiEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val coverUrl: String,
    val region: String?,
    val zoom: Float?,
    val cachedAt: Long,
    val isCached: Boolean = false
) {
    fun toDomain() = Bangumi(
        id = id,
        title = title,
        coverUrl = coverUrl,
        region = region,
        zoom = zoom
    )

    companion object {
        fun fromDomain(domain: Bangumi, cachedAt: Long = System.currentTimeMillis()) = BangumiEntity(
            id = domain.id,
            title = domain.title,
            coverUrl = domain.coverUrl,
            region = domain.region,
            zoom = domain.zoom,
            cachedAt = cachedAt
        )
    }
}
