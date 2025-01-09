package com.example.navigationtoyproject.api.model

data class MapDataDto(
    val customLocationMarkerId: Int,
    val wgs84_x: Double,
    val wgs84_y: Double,
    val mapVersion: String
)