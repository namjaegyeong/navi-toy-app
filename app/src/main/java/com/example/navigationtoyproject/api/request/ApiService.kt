package com.example.navigationtoyproject.api.request

import com.example.navigationtoyproject.api.model.MapDataListDto
import com.example.navigationtoyproject.api.model.ResponseMapVersionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("map-version")
    suspend fun findRecentMapVersion(): Response<ResponseMapVersionDto>

    @POST("map-data")
    suspend fun findMapDataList(@Body responseMapVersionDto: ResponseMapVersionDto): Response<MapDataListDto>
}