package com.example.navigationtoyproject.datastore

import com.example.navigationtoyproject.MapData
import com.example.navigationtoyproject.api.model.MapDataDto

object MapDataMapper {

    private fun dtoToProto(dto: MapDataDto): MapData {
        return MapData.newBuilder()
            .setCustomLocationMarkerId(dto.customLocationMarkerId)
            .setWgs84X(dto.wgs84_x)
            .setWgs84Y(dto.wgs84_y)
            .setMapVersion(dto.mapVersion)
            .build()
    }

    private fun protoToDto(proto: MapData): MapDataDto {
        return MapDataDto(
            customLocationMarkerId = proto.customLocationMarkerId,
            wgs84_x = proto.wgs84X,
            wgs84_y = proto.wgs84Y,
            mapVersion = proto.mapVersion
        )
    }

    fun dtoListToProtoList(dtoList: List<MapDataDto>): List<MapData> {
        return dtoList.map { dtoToProto(it) }
    }

    fun protoListToDtoList(protoList: List<MapData>): List<MapDataDto> {
        return protoList.map { protoToDto(it) }
    }
}