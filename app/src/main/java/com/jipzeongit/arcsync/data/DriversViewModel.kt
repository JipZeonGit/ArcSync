package com.jipzeongit.arcsync.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DriversUiState {
    data object Loading : DriversUiState()
    data class Error(val message: String) : DriversUiState()
    data class Data(val drivers: List<DriverSummary>) : DriversUiState()
}

sealed class DriverDetailState {
    data object Loading : DriverDetailState()
    data class Error(val message: String) : DriverDetailState()
    data class Data(val detail: DriverDetail) : DriverDetailState()
}

class DriversViewModel(
    private val repository: IntelArcRepository = IntelArcRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<DriversUiState>(DriversUiState.Loading)
    val uiState: StateFlow<DriversUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _detailState = MutableStateFlow<DriverDetailState>(DriverDetailState.Loading)
    val detailState: StateFlow<DriverDetailState> = _detailState.asStateFlow()

    private var allDrivers: List<DriverSummary> = emptyList()

    fun loadDrivers() {
        if (_uiState.value is DriversUiState.Data) return
        _uiState.value = DriversUiState.Loading
        viewModelScope.launch {
            try {
                allDrivers = repository.fetchAllDrivers()
                _uiState.value = DriversUiState.Data(allDrivers)
            } catch (t: Throwable) {
                _uiState.value = DriversUiState.Error(t.message ?: "Unknown error")
            }
        }
    }

    fun refreshDrivers() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.clearCache()
                allDrivers = repository.fetchAllDrivers()
                _uiState.value = DriversUiState.Data(allDrivers)
            } catch (t: Throwable) {
                _uiState.value = DriversUiState.Error(t.message ?: "Unknown error")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadDetail(detailUrl: String) {
        _detailState.value = DriverDetailState.Loading
        viewModelScope.launch {
            try {
                val detail = repository.fetchDriverDetail(detailUrl)
                _detailState.value = DriverDetailState.Data(detail)
            } catch (t: Throwable) {
                _detailState.value = DriverDetailState.Error(t.message ?: "Unknown error")
            }
        }
    }
}
