package com.tnt.seichicamera.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tnt.seichicamera.domain.model.CheckIn

@Entity(
    tableName = "check_in",
    indices = [Index("pointId")]
)
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pointId: String,
    val photoUri: String,
    val timestamp: Long,
    val comparisonUri: String?
) {
    fun toDomain() = CheckIn(
        id = id,
        pointId = pointId,
        photoUri = photoUri,
        timestamp = timestamp,
        comparisonUri = comparisonUri
    )

    companion object {
        fun fromDomain(domain: CheckIn) = CheckInEntity(
            id = domain.id,
            pointId = domain.pointId,
            photoUri = domain.photoUri,
            timestamp = domain.timestamp,
            comparisonUri = domain.comparisonUri
        )
    }
}
