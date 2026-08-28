package com.tnt.seichicamera.data.remote

import com.tnt.seichicamera.data.remote.dto.BangumiResponse
import com.tnt.seichicamera.data.remote.dto.PointDetailItem
import retrofit2.http.GET
import retrofit2.http.Path

interface AnitabiApi {
    companion object {
        const val BASE_URL = "https://api.anitabi.cn/"
    }

    /**
     * Lite endpoint: returns bangumi metadata + up to 10 sample points.
     * Used only for metadata (title, cover, zoom, city).
     */
    @GET("bangumi/{subjectId}/lite")
    suspend fun getBangumiLite(@Path("subjectId") subjectId: Int): BangumiResponse

    /**
     * Full points endpoint: returns ALL sacred points for a bangumi.
     * Returns a JSON array of PointDetailItem directly.
     */
    @GET("bangumi/{subjectId}/points/detail")
    suspend fun getBangumiPoints(@Path("subjectId") subjectId: Int): List<PointDetailItem>
}
