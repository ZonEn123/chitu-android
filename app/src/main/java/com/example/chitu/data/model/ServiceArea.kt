package com.example.chitu.data.model

data class ServiceArea(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Float,
    val type: String,
    val province: String,
    val city: String
) {
    fun getFormattedDistance(): String {
        return when {
            distance < 1000 -> "${distance.toInt()}米"
            else -> String.format("%.1f公里", distance / 1000)
        }
    }
}
