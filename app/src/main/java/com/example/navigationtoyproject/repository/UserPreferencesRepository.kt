package com.example.navigationtoyproject.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.navigationtoyproject.api.model.NetworkResult
import com.example.navigationtoyproject.repository.UserPreferencesRepository.PreferenceKeys.MAP_VERSION
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.io.IOException

class UserPreferencesRepository(
    private val userPreferencesStore: DataStore<Preferences>
) {
    private object PreferenceKeys {
        val MAP_VERSION = stringPreferencesKey("map_version")
    }

    val getMapVersionFlow: Flow<String?> = userPreferencesStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[MAP_VERSION]
        }

    suspend fun updateMapVersion(mapVersion: String) {
        userPreferencesStore.edit { preferences ->
            preferences[MAP_VERSION] = mapVersion
        }
    }

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