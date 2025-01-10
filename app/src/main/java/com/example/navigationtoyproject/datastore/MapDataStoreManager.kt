package com.example.navigationtoyproject.datastore

import android.content.Context
import androidx.datastore.dataStore
import com.example.navigationtoyproject.MapData
import com.example.navigationtoyproject.MapDataList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

object MapDataStoreManager {
    private val Context.mapDataStore by dataStore(
        fileName = "map_data.pb",
        serializer = MapDataStoreSerializer
    )

    fun getMapDataList(context: Context): Flow<MapDataList?> {
        return context.mapDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(MapDataList.getDefaultInstance())
                } else {
                    throw exception
                }
            }
            .map { mapDataList ->
                mapDataList
            }
    }

    // Function to add a new MapDataDto to the list
    suspend fun addMapData(context: Context, mapData: MapData) {
        context.mapDataStore.updateData { currentData ->
            currentData.toBuilder()
                .addMapDataList(mapData)
                .build()
        }
    }

    // Function to set the entire list
    suspend fun setMapDataList(context: Context, newList: List<MapData>) {
        context.mapDataStore.updateData { currentData ->
            currentData.toBuilder()
                .clearMapDataList()
                .addAllMapDataList(newList)
                .build()
        }
    }

    // Function to clear the list
    suspend fun clearMapDataList(context: Context) {
        context.mapDataStore.updateData { currentData ->
            currentData.toBuilder()
                .clearMapDataList()
                .build()
        }
    }
}