package com.sahed.xblocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahed.xblocker.data.datastore.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: AppPreferences
) : ViewModel() {

    /**
     * Null means "still loading" — used to avoid a flicker between onboarding
     * and main nav while the DataStore value is being read asynchronously.
     */
    val isOnboardingDone: StateFlow<Boolean?> = preferences.isOnboardingDone
        .map<Boolean, Boolean?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null // null = loading
        )

    val isDarkMode: StateFlow<Boolean> = preferences.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )
}
