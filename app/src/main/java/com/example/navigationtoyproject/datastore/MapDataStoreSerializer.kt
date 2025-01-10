package com.example.navigationtoyproject.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import com.example.navigationtoyproject.MapDataList
import java.io.InputStream
import java.io.OutputStream

object MapDataStoreSerializer : Serializer<MapDataList> {
    override val defaultValue: MapDataList = MapDataList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): MapDataList {
        try {
            return MapDataList.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: MapDataList, output: OutputStream) = t.writeTo(output)
}