package com.tnt.seichicamera.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tnt.seichicamera.data.repository.BangumiRepository
import com.tnt.seichicamera.domain.model.Bangumi
import com.tnt.seichicamera.util.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentLocaleTag: String = "",
    val cachedBangumis: List<Bangumi> = emptyList(),
    val isLoadingCache: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val bangumiRepository: BangumiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(
        currentLocaleTag = LocaleHelper.getCurrentLocaleTag()
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCachedBangumis()
    }

    fun setLanguage(tag: String) {
        LocaleHelper.setLocale(tag)
        _uiState.update { it.copy(currentLocaleTag = tag) }
    }

    fun loadCachedBangumis() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCache = true) }
            val cached = bangumiRepository.getCachedBangumis()
            _uiState.update { it.copy(cachedBangumis = cached, isLoadingCache = false) }
        }
    }

    fun clearCache(subjectId: Int) {
        viewModelScope.launch {
            bangumiRepository.clearCache(subjectId)
            loadCachedBangumis()
        }
    }

    fun clearAllCache() {
        viewModelScope.launch {
            bangumiRepository.clearAllCache()
            loadCachedBangumis()
        }
    }
}
