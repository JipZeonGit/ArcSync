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
    data class Data(
        val drivers: List<DriverSummary>,
        val canLoadMore: Boolean
    ) : DriversUiState()
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

    private val _detailState = MutableStateFlow<DriverDetailState>(DriverDetailState.Loading)
    val detailState: StateFlow<DriverDetailState> = _detailState.asStateFlow()

    private var allDrivers: List<DriverSummary> = emptyList()
    private var visibleCount = 0

    fun loadDrivers() {
        if (_uiState.value is DriversUiState.Data) return
        _uiState.value = DriversUiState.Loading
        viewModelScope.launch {
            try {
                allDrivers = repository.fetchAllDrivers()
                visibleCount = INITIAL_VISIBLE.coerceAtMost(allDrivers.size)
                emitVisible()
            } catch (t: Throwable) {
                _uiState.value = DriversUiState.Error(t.message ?: "Unknown error")
            }
        }
    }

    fun loadMore() {
        if (allDrivers.isEmpty()) return
        val next = (visibleCount + PAGE_SIZE).coerceAtMost(allDrivers.size)
        if (next == visibleCount) return
        visibleCount = next
        emitVisible()
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

    private fun emitVisible() {
        val slice = allDrivers.take(visibleCount)
        _uiState.value = DriversUiState.Data(
            drivers = slice,
            canLoadMore = visibleCount < allDrivers.size
        )
    }

    companion object {
        private const val INITIAL_VISIBLE = 6
        private const val PAGE_SIZE = 6
    }
}
