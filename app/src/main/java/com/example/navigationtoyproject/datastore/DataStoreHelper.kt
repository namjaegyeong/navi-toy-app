package com.example.navigationtoyproject.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.example.navigationtoyproject.datastore.DataStoreManager.dataStore
import com.example.navigationtoyproject.datastore.PreferenceKeys.MAP_VERSION_KEY
import com.example.navigationtoyproject.datastore.PreferenceKeys.REQUEST_FLAG_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

object DataStoreHelper {
    // Function to get Map Version as a Flow
    fun getMapVersion(context: Context): Flow<String?> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[MAP_VERSION_KEY]
            }
    }

    // Function to set Map Version
    suspend fun setMapVersion(context: Context, mapVersion: String) {
        context.dataStore.edit { preferences ->
            preferences[MAP_VERSION_KEY] = mapVersion
        }
    }

    // Function to get Request Flag as a Flow
    fun getRequestFlag(context: Context): Flow<Int?> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[REQUEST_FLAG_KEY]
            }
    }

    // Function to set Request Flag
    suspend fun setRequestFlag(context: Context, flag: Int) {
        context.dataStore.edit { preferences ->
            preferences[REQUEST_FLAG_KEY] = flag
        }
    }

    // Function to reset Request Flag to 0
    suspend fun resetRequestFlag(context: Context) {
        context.dataStore.edit { preferences ->
            preferences[REQUEST_FLAG_KEY] = 0
        }
    }
}