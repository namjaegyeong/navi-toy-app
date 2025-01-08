package com.example.navigationtoyproject.api.request

import com.example.navigationtoyproject.api.model.ResponseMapVersionDto
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("map-version")
    suspend fun findRecentMapVersion(): Response<ResponseMapVersionDto>
}