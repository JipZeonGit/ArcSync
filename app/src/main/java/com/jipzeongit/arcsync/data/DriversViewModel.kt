package com.jipzeongit.arcsync.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DriversUiState {
    data object Loading : DriversUiState()
    data class Data(val drivers: List<DriverSummary>) : DriversUiState()
    data class Error(val message: String) : DriversUiState()
}

sealed class DriverDetailState {
    data object Loading : DriverDetailState()
    data class Data(val detail: DriverDetail) : DriverDetailState()
    data class Error(val message: String) : DriverDetailState()
}

class DriversViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IntelArcRepository(application)

    private val _uiState = MutableStateFlow<DriversUiState>(DriversUiState.Loading)
    val uiState: StateFlow<DriversUiState> = _uiState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _detailState = MutableStateFlow<DriverDetailState>(DriverDetailState.Loading)
    val detailState: StateFlow<DriverDetailState> = _detailState

    private var allDrivers = emptyList<DriverSummary>()
    private var currentLang: AppLang = AppLang.ZH_CN

    fun loadDrivers(lang: AppLang = currentLang) {
        if (allDrivers.isNotEmpty() && lang == currentLang) {
            _uiState.value = DriversUiState.Data(allDrivers)
            return
        }
        currentLang = lang
        _uiState.value = DriversUiState.Loading
        viewModelScope.launch {
            try {
                allDrivers = repository.fetchAllDrivers(lang)
                _uiState.value = DriversUiState.Data(allDrivers)
            } catch (t: Throwable) {
                if (allDrivers.isNotEmpty()) {
                    _uiState.value = DriversUiState.Data(allDrivers)
                } else {
                    _uiState.value = DriversUiState.Error(t.message ?: "Unknown error")
                }
            }
        }
    }

    fun refreshDrivers(lang: AppLang = currentLang) {
        if (_isRefreshing.value) return

        _isRefreshing.value = true
        currentLang = lang
        val previousDrivers = allDrivers
        repository.clearCache()
        viewModelScope.launch {
            try {
                allDrivers = repository.fetchAllDrivers(lang)
                _uiState.value = DriversUiState.Data(allDrivers)
            } catch (t: Throwable) {
                if (previousDrivers.isNotEmpty()) {
                    allDrivers = previousDrivers
                    _uiState.value = DriversUiState.Data(previousDrivers)
                } else {
                    _uiState.value = DriversUiState.Error(t.message ?: "Unknown error")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearAndReload(lang: AppLang = currentLang) {
        val previousDrivers = allDrivers
        repository.clearCache()
        currentLang = lang
        _uiState.value = DriversUiState.Loading
        viewModelScope.launch {
            try {
                allDrivers = repository.fetchAllDrivers(lang)
                _uiState.value = DriversUiState.Data(allDrivers)
            } catch (t: Throwable) {
                if (previousDrivers.isNotEmpty()) {
                    allDrivers = previousDrivers
                    _uiState.value = DriversUiState.Data(previousDrivers)
                } else {
                    allDrivers = emptyList()
                    _uiState.value = DriversUiState.Error(t.message ?: "Unknown error")
                }
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
