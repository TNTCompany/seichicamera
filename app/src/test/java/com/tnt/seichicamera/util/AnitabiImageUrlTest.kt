package com.tnt.seichicamera.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AnitabiImageUrlTest {

    @Test
    fun `withAnitabiImagePlan replaces the thumbnail plan`() {
        val thumbnailUrl =
            "https://image.anitabi.cn/points/115908/point.jpg?plan=h160"

        val result = thumbnailUrl.withAnitabiImagePlan("h360")

        assertEquals(
            "https://image.anitabi.cn/points/115908/point.jpg?plan=h360",
            result
        )
    }

    @Test
    fun `withAnitabiImagePlan adds a plan when a cached URL has none`() {
        val cachedUrl = "https://image.anitabi.cn/points/115908/point.jpg"

        val result = cachedUrl.withAnitabiImagePlan("h360")

        assertEquals(
            "https://image.anitabi.cn/points/115908/point.jpg?plan=h360",
            result
        )
    }

    @Test
    fun `withAnitabiImagePlan leaves non Anitabi image URLs unchanged`() {
        val externalUrl = "https://images.example.com/reference.jpg"

        val result = externalUrl.withAnitabiImagePlan("h360")

        assertEquals(externalUrl, result)
    }

    @Test
    fun `withAnitabiImagePlan preserves existing query parameters and fragments`() {
        val cachedUrl =
            "https://image.anitabi.cn/points/115908/point.jpg?token=abc#frame"

        val result = cachedUrl.withAnitabiImagePlan("h360")

        assertEquals(
            "https://image.anitabi.cn/points/115908/point.jpg?token=abc&plan=h360#frame",
            result
        )
    }
}
