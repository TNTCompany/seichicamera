package com.tnt.seichicamera.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tnt.seichicamera.domain.model.SacredPoint

@Entity(
    tableName = "sacred_point",
    foreignKeys = [ForeignKey(
        entity = BangumiEntity::class,
        parentColumns = ["id"],
        childColumns = ["bangumiId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bangumiId")]
)
data class SacredPointEntity(
    @PrimaryKey val id: String,
    val bangumiId: Int,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val imageUrls: List<String>,
    val originUrl: String?,
    val ep: String?
) {
    fun toDomain() = SacredPoint(
        id = id,
        bangumiId = bangumiId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        imageUrls = imageUrls,
        originUrl = originUrl,
        ep = ep
    )

    companion object {
        fun fromDomain(domain: SacredPoint) = SacredPointEntity(
            id = domain.id,
            bangumiId = domain.bangumiId,
            name = domain.name,
            latitude = domain.latitude,
            longitude = domain.longitude,
            imageUrls = domain.imageUrls,
            originUrl = domain.originUrl,
            ep = domain.ep
        )
    }
}
