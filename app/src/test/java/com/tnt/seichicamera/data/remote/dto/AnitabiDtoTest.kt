package com.tnt.seichicamera.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnitabiDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sampleLiteJson = """
        {
            "id": 204135,
            "cn": "摇曳露营△",
            "title": "ゆるキャン△",
            "cover": "abc123.jpg",
            "zoom": 10.0,
            "city": "山梨県"
        }
    """.trimIndent()
    
    private val samplePointDetailJson = """
        [
            {
                "id": "abc_123",
                "cn": "本栖湖",
                "name": "Motosuko",
                "image": "img001.jpg",
                "ep": "EP01",
                "s": 120,
                "geo": [35.4500, 138.5833],
                "origin": "Twitter",
                "originURL": "https://anitabi.cn/map?id=204135"
            },
            {
                "id": "def_456",
                "name": "浩庵キャンプ場",
                "geo": [35.4600, 138.5700],
                "image": "img002.jpg",
                "ep": 1
            }
        ]
    """.trimIndent()

    @Test
    fun `parse BangumiResponse from JSON`() {
        val response = json.decodeFromString<BangumiResponse>(sampleLiteJson)
        assertEquals(204135, response.id)
        assertEquals("摇曳露营△", response.titleCn)
        assertEquals("ゆるキャン△", response.title)
    }

    @Test
    fun `BangumiResponse maps to BangumiEntity with CN title preferred`() {
        val response = json.decodeFromString<BangumiResponse>(sampleLiteJson)
        val entity = response.toBangumiEntity()
        assertEquals(204135, entity.id)
        assertEquals("摇曳露营△", entity.title)
        assertEquals("山梨県", entity.region)
        assertEquals("https://image.anitabi.cn/abc123.jpg", entity.coverUrl)
    }

    @Test
    fun `PointDetailItem maps to SacredPointEntity`() {
        val points = json.decodeFromString<List<PointDetailItem>>(samplePointDetailJson)
        val entities = points.map { it.toEntity(204135) }
        assertEquals(2, entities.size)

        val first = entities[0]
        assertEquals("abc_123", first.id)
        assertEquals("本栖湖", first.name)
        assertEquals(35.4500, first.latitude, 0.001)
        assertEquals(138.5833, first.longitude, 0.001)
        assertEquals("https://image.anitabi.cn/img001.jpg", first.imageUrls[0])
        assertEquals("EP01", first.ep)
        
        val second = entities[1]
        assertEquals("1", second.ep)
    }
}
