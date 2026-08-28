package com.tnt.seichicamera.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tnt.seichicamera.R
import com.tnt.seichicamera.data.remote.BangumiSearchApi
import com.tnt.seichicamera.data.remote.dto.BangumiSearchItem
import com.tnt.seichicamera.data.repository.BangumiRepository
import com.tnt.seichicamera.data.repository.CheckInRepository
import com.tnt.seichicamera.domain.model.Bangumi
import com.tnt.seichicamera.domain.model.BangumiSearchResult
import com.tnt.seichicamera.domain.model.SacredPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val errorArg: String? = null,
    val searchResults: List<BangumiSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val showSearchResults: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val bangumiRepository: BangumiRepository,
    private val checkInRepository: CheckInRepository,
    private val bangumiSearchApi: BangumiSearchApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    val checkedInPointIds: StateFlow<List<String>> =
        checkInRepository.getCheckedInPointIds()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    companion object {
        // Popular anime for first-time users (Bangumi Subject IDs)
        private val DEFAULT_SUBJECT_ID = 207195 // ゆるキャン△ (Laid-Back Camp)
    }

    init {
        loadDefaultContent()
    }

    private fun loadDefaultContent() {
        viewModelScope.launch {
            // Try loading the most recently cached bangumi first
            val cached = bangumiRepository.getCachedBangumis()
            if (cached.isNotEmpty()) {
                val latest = cached.first()
                loadBangumiPoints(latest.id)
            } else {
                // First-time user: load a popular anime
                loadBangumiPoints(DEFAULT_SUBJECT_ID)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        // Debounced name search
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), showSearchResults = false, isSearching = false) }
            return
        }

        // If it's a pure number, don't trigger name search (user is typing a Bangumi ID)
        if (query.trim().toIntOrNull() != null) {
            _uiState.update { it.copy(searchResults = emptyList(), showSearchResults = false, isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // debounce 500ms
            _uiState.update { it.copy(isSearching = true) }
            try {
                val response = bangumiSearchApi.searchSubjects(query.trim())
                val results = response.list?.map { it.toDomain() } ?: emptyList()
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        showSearchResults = results.isNotEmpty(),
                        isSearching = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false, searchResults = emptyList(), showSearchResults = false) }
            }
        }
    }

    fun selectSearchResult(result: BangumiSearchResult) {
        _uiState.update {
            it.copy(
                searchQuery = result.nameCn ?: result.name,
                searchResults = emptyList(),
                showSearchResults = false
            )
        }
        loadBangumiPoints(result.id)
    }

    fun dismissSearchResults() {
        _uiState.update { it.copy(showSearchResults = false) }
    }

    fun searchBangumi() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return

        // Dismiss any open search dropdown
        _uiState.update { it.copy(showSearchResults = false) }
        searchJob?.cancel()

        val subjectId = query.toIntOrNull()
        if (subjectId != null) {
            loadBangumiPoints(subjectId)
        } else {
            // User pressed search with text query — trigger name search
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null, errorRes = null, errorArg = null) }
                try {
                    val response = bangumiSearchApi.searchSubjects(query)
                    val results = response.list?.map { it.toDomain() } ?: emptyList()
                    if (results.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                searchResults = results,
                                showSearchResults = true,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorRes = R.string.search_no_results
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorRes = R.string.error_search_failed,
                            errorArg = e.message ?: "Unknown"
                        )
                    }
                }
            }
        }
    }

    private fun loadBangumiPoints(subjectId: Int) {
        _uiState.update { it.copy(isLoading = true, error = null, errorRes = null, errorArg = null) }
        viewModelScope.launch {
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

    private fun BangumiSearchItem.toDomain() = BangumiSearchResult(
        id = id,
        name = name ?: "Unknown",
        nameCn = nameCn,
        imageUrl = images?.grid ?: images?.small,
        airDate = airDate
    )
}
