package com.example.chitu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitu.data.model.LocationResult
import com.example.chitu.data.repository.LocationRepository
import com.example.chitu.data.repository.ServiceAreaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServiceAreaViewModel(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val repository = ServiceAreaRepository()
    private val _uiState = MutableStateFlow<ServiceAreaUiState>(ServiceAreaUiState.Idle)
    val uiState: StateFlow<ServiceAreaUiState> = _uiState.asStateFlow()

    private var cachedLocation: Pair<Double, Double>? = null

    /** 自动定位并搜索（页面入口） */
    fun loadServiceAreas() {
        viewModelScope.launch {
            _uiState.value = ServiceAreaUiState.Loading
            when (val loc = locationRepository.getCurrentLocation()) {
                is LocationResult.Success -> {
                    cachedLocation = Pair(loc.latitude, loc.longitude)
                    searchServiceAreas(loc.latitude, loc.longitude)
                }
                is LocationResult.PermissionDenied ->
                    _uiState.value = ServiceAreaUiState.Error("需要定位权限才能查询附近服务区")
                is LocationResult.Error ->
                    _uiState.value = ServiceAreaUiState.Error(loc.message)
                else -> {}
            }
        }
    }

    private fun searchServiceAreas(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = ServiceAreaUiState.Loading
            delay(300)
            when (val result = repository.searchNearbyServiceAreas(latitude, longitude)) {
                null -> _uiState.value = ServiceAreaUiState.Error("搜索失败，请检查网络后重试")
                emptyList<Any>() -> _uiState.value = ServiceAreaUiState.Empty
                else -> _uiState.value = ServiceAreaUiState.Success(result)
            }
        }
    }

    fun refresh() {
        cachedLocation?.let { (lat, lng) -> searchServiceAreas(lat, lng) }
            ?: loadServiceAreas()
    }

    fun reset() {
        _uiState.value = ServiceAreaUiState.Idle
    }
}
