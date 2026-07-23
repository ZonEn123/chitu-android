package com.example.chitu.data.remote.dto

data class AmapPoiResponse(
    val status: String,
    val info: String,
    val count: String,
    val pois: List<AmapPoi>
)

data class AmapPoi(
    val id: String,
    val name: String,
    val address: String,
    val location: String,
    val distance: String,
    val pname: String,
    val cityname: String,
    val type: String
)
