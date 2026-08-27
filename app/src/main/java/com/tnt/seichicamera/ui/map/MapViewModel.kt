package com.tnt.seichicamera.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tnt.seichicamera.R
import com.tnt.seichicamera.data.repository.BangumiRepository
import com.tnt.seichicamera.data.repository.CheckInRepository
import com.tnt.seichicamera.domain.model.Bangumi
import com.tnt.seichicamera.domain.model.SacredPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val searchQuery: String = "",
    val bangumi: Bangumi? = null,
    val points: List<SacredPoint> = emptyList(),
    val selectedPoint: SacredPoint? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val errorRes: Int? = null,
    val errorArg: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val bangumiRepository: BangumiRepository,
    private val checkInRepository: CheckInRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    val checkedInPointIds: StateFlow<List<String>> =
        checkInRepository.getCheckedInPointIds()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun searchBangumi() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null, errorRes = null, errorArg = null) }
        viewModelScope.launch {
            // Try parsing as Bangumi ID (number)
            val subjectId = query.toIntOrNull()
            if (subjectId != null) {
                val result = bangumiRepository.getBangumiPoints(subjectId)
                result.fold(
                    onSuccess = { (bangumi, points) ->
                        _uiState.update {
                            it.copy(
                                bangumi = bangumi,
                                points = points,
                                isLoading = false,
                                selectedPoint = null
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message,
                                errorRes = if (e.message == null) R.string.error_unknown else null
                            )
                        }
                    }
                )
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorRes = R.string.error_enter_bangumi_id,
                        error = null
                    )
                }
            }
        }
    }

    fun selectPoint(point: SacredPoint?) {
        _uiState.update { it.copy(selectedPoint = point) }
    }

    fun downloadOfflineCache() {
        if (_uiState.value.isLoading) return
        val subjectId = _uiState.value.bangumi?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = bangumiRepository.cacheOffline(subjectId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, error = null, errorRes = null, errorArg = null) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorRes = R.string.error_cache_failed,
                            errorArg = e.message ?: "Unknown"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, errorRes = null, errorArg = null) }
    }
}
