package com.tnt.seichicamera.data.remote

import com.tnt.seichicamera.data.remote.dto.BangumiSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BangumiSearchApi {
    companion object {
        const val BASE_URL = "https://api.bgm.tv/"
    }

    @GET("search/subject/{keywords}")
    suspend fun searchSubjects(
        @Path("keywords") keywords: String,
        @Query("type") type: Int = 2,
        @Query("responseGroup") responseGroup: String = "small",
        @Query("max_results") maxResults: Int = 10
    ): BangumiSearchResponse
}
