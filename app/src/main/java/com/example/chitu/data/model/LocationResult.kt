package com.example.chitu.data.model

sealed class LocationResult {
    object Loading : LocationResult()
    data class Success(val latitude: Double, val longitude: Double) : LocationResult()
    data class Error(val message: String) : LocationResult()
    object PermissionDenied : LocationResult()
}
