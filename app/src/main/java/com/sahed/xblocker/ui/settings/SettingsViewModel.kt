package com.sahed.xblocker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahed.xblocker.data.datastore.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val defaultDisableDuration: Long = 3 * 60 * 60 * 1000L
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.isDarkMode.collect { enabled ->
                _state.value = _state.value.copy(isDarkMode = enabled)
            }
        }
        viewModelScope.launch {
            preferences.defaultDisableDuration.collect { duration ->
                _state.value = _state.value.copy(defaultDisableDuration = duration)
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkMode(enabled) }
    }

    fun setDefaultDisableDuration(duration: Long) {
        viewModelScope.launch { preferences.setDefaultDisableDuration(duration) }
    }
}
