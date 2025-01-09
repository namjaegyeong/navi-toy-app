package com.example.navigationtoyproject.repository

import com.example.navigationtoyproject.api.model.NetworkResult
import retrofit2.Response

class UserPreferencesRepository {
    suspend fun <T : Any> handleApi(
        execute: suspend () -> Response<T>
    ): NetworkResult<T> {
        return try {
            val response = execute()
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    NetworkResult.Success(dto)
                } else {
                    NetworkResult.Error(message = "User data is null")
                }
            } else {
                NetworkResult.Error(message = "API Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error(message = e.message)
        }
    }
}