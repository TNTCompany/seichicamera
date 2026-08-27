package com.tnt.seichicamera.data.remote

import com.tnt.seichicamera.data.remote.dto.BangumiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface AnitabiApi {
    companion object {
        const val BASE_URL = "https://api.anitabi.cn/"
    }

    @GET("bangumi/{subjectId}/lite")
    suspend fun getBangumiPoints(@Path("subjectId") subjectId: Int): BangumiResponse
}
