package com.tnt.seichicamera.domain.model

import com.tnt.seichicamera.data.local.entity.BangumiEntity
import com.tnt.seichicamera.data.local.entity.CheckInEntity
import com.tnt.seichicamera.data.local.entity.SacredPointEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelMappingTest {

    @Test
    fun `BangumiEntity round-trips through domain model`() {
        val domain = Bangumi(id = 1, title = "Steins;Gate", coverUrl = "https://img.example.com/cover.jpg", region = "Tokyo", zoom = 15f)
        val entity = BangumiEntity.fromDomain(domain, cachedAt = 1000L)
        val result = entity.toDomain()
        assertEquals(domain, result)
    }

    @Test
    fun `SacredPointEntity round-trips through domain model`() {
        val domain = SacredPoint(
            id = "point-1", bangumiId = 1, name = "Akihabara Radio Kaikan",
            latitude = 35.6984, longitude = 139.7714,
            imageUrls = listOf("https://img.example.com/1.jpg", "https://img.example.com/2.jpg"),
            originUrl = "https://anitabi.cn/point/1", ep = "EP01"
        )
        val entity = SacredPointEntity.fromDomain(domain)
        val result = entity.toDomain()
        assertEquals(domain, result)
    }

    @Test
    fun `CheckInEntity round-trips through domain model`() {
        val domain = CheckIn(id = 0, pointId = "point-1", photoUri = "content://media/photo/1", timestamp = 1000L, comparisonUri = null)
        val entity = CheckInEntity.fromDomain(domain)
        val result = entity.toDomain()
        assertEquals(domain, result)
    }
}
