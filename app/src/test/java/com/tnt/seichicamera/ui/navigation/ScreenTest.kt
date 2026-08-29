package com.tnt.seichicamera.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTest {

    @Test
    fun `camera route encodes image URLs and point ID as query parameter values`() {
        val imageUrls =
            "https://image.anitabi.cn/point.jpg?plan=h360&token=abc,https://example.com/2.jpg"

        val route = Screen.Camera.createRoute(
            imageUrls = imageUrls,
            pointId = "point&1"
        )

        assertEquals(
            "camera?imageUrls=https%3A%2F%2Fimage.anitabi.cn%2Fpoint.jpg%3Fplan%3Dh360%26token%3Dabc%2Chttps%3A%2F%2Fexample.com%2F2.jpg&pointId=point%261",
            route
        )
    }
}
