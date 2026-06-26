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
    val isDefaultListEnabled: Boolean = true,
    val isAutoStartBoot: Boolean = true,
    val upstreamDns: String = "1.1.1.1"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.isDefaultListEnabled.collect { enabled ->
                _state.value = _state.value.copy(isDefaultListEnabled = enabled)
            }
        }
        viewModelScope.launch {
            preferences.isAutoStartBoot.collect { enabled ->
                _state.value = _state.value.copy(isAutoStartBoot = enabled)
            }
        }
        viewModelScope.launch {
            preferences.upstreamDns.collect { dns ->
                _state.value = _state.value.copy(upstreamDns = dns)
            }
        }
    }

    fun setDefaultListEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setDefaultListEnabled(enabled) }
    }

    fun setAutoStartBoot(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoStartBoot(enabled) }
    }

    fun setUpstreamDns(dns: String) {
        viewModelScope.launch { preferences.setUpstreamDns(dns) }
    }
}
