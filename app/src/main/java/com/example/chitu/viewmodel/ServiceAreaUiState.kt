package com.example.chitu.viewmodel

import com.example.chitu.data.model.ServiceArea

sealed class ServiceAreaUiState {
    object Idle : ServiceAreaUiState()
    object Loading : ServiceAreaUiState()
    data class Success(val data: List<ServiceArea>) : ServiceAreaUiState()
    data class Error(val message: String) : ServiceAreaUiState()
    object Empty : ServiceAreaUiState()
}
