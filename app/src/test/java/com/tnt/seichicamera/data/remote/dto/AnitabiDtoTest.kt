package com.tnt.seichicamera.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnitabiDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sampleJson = """
        {
            "id": 204135,
            "cn": "摇曳露营△",
            "title": "ゆるキャン△",
            "cover": "abc123.jpg",
            "zoom": 10.0,
            "city": "山梨県",
            "litePoints": [
                {
                    "name": "本栖湖",
                    "geo": [138.5833, 35.4500],
                    "image": "img001.jpg",
                    "ep": "EP01",
                    "origin": "https://anitabi.cn/map?id=204135"
                },
                {
                    "name": "浩庵キャンプ場",
                    "geo": [138.5700, 35.4600],
                    "image": "img002.jpg",
                    "ep": "EP01"
                }
            ]
        }
    """.trimIndent()

    @Test
    fun `parse BangumiResponse from JSON`() {
        val response = json.decodeFromString<BangumiResponse>(sampleJson)
        assertEquals(204135, response.id)
        assertEquals("摇曳露营△", response.titleCn)
        assertEquals("ゆるキャン△", response.title)
        assertEquals(2, response.litePoints?.size)
    }

    @Test
    fun `BangumiResponse maps to BangumiEntity with CN title preferred`() {
        val response = json.decodeFromString<BangumiResponse>(sampleJson)
        val entity = response.toBangumiEntity()
        assertEquals(204135, entity.id)
        assertEquals("摇曳露营△", entity.title)
        assertEquals("山梨県", entity.region)
    }

    @Test
    fun `BangumiResponse maps litePoints to SacredPointEntities`() {
        val response = json.decodeFromString<BangumiResponse>(sampleJson)
        val points = response.toPointEntities()
        assertEquals(2, points.size)

        val first = points[0]
        assertEquals("204135_0", first.id)
        assertEquals("本栖湖", first.name)
        assertEquals(35.4500, first.latitude, 0.001)
        assertEquals(138.5833, first.longitude, 0.001)
        assertTrue(first.imageUrls[0].contains("?plan=h360"))
        assertEquals("EP01", first.ep)
    }

    @Test
    fun `BangumiResponse with no litePoints returns empty list`() {
        val response = json.decodeFromString<BangumiResponse>("""{"id": 1}""")
        assertEquals(0, response.toPointEntities().size)
    }
}
