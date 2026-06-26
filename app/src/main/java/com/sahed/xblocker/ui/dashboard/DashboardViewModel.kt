package com.sahed.xblocker.ui.dashboard

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahed.xblocker.data.datastore.AppPreferences
import com.sahed.xblocker.data.repository.BlocklistRepository
import com.sahed.xblocker.data.repository.TimerRepository
import com.sahed.xblocker.domain.model.TimerEvent
import com.sahed.xblocker.domain.usecase.CancelTimerUseCase
import com.sahed.xblocker.domain.usecase.RequestDisableBlockerUseCase
import com.sahed.xblocker.service.XBlockerAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isBlockerActive: Boolean = false,
    val isAccessibilityServiceRunning: Boolean = false,
    val blockedToday: Int = 0,
    val customDomainCount: Int = 0,
    val totalBlockedCount: Int = 0,
    val activeDisableTimer: TimerEvent? = null,
    val timerRemainingMs: Long = 0L,
    val defaultDisableDuration: Long = 3 * 60 * 60 * 1000L
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
    private val blocklistRepo: BlocklistRepository,
    private val timerRepo: TimerRepository,
    private val requestDisableBlocker: RequestDisableBlockerUseCase,
    private val cancelTimer: CancelTimerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val defaultListSize: Int = 500 // placeholder — real count loaded from asset

    init {
        observeState()
        tickTimer()
        pollServiceRunningState()
    }

    private fun observeState() {
        viewModelScope.launch {
            preferences.isBlockerActive.collect { active ->
                _uiState.value = _uiState.value.copy(isBlockerActive = active)
            }
        }
        viewModelScope.launch {
            preferences.blockedToday.collect { count ->
                _uiState.value = _uiState.value.copy(blockedToday = count)
            }
        }
        viewModelScope.launch {
            blocklistRepo.getActiveCustomCount().collect { count ->
                _uiState.value = _uiState.value.copy(customDomainCount = count)
            }
        }
        viewModelScope.launch {
            blocklistRepo.getTotalBlockedCount().collect { count ->
                _uiState.value = _uiState.value.copy(totalBlockedCount = count ?: 0)
            }
        }
        viewModelScope.launch {
            timerRepo.getActiveDisableTimer().collect { timer ->
                _uiState.value = _uiState.value.copy(
                    activeDisableTimer = timer,
                    timerRemainingMs = timer?.remainingMs() ?: 0L
                )
            }
        }
        viewModelScope.launch {
            preferences.defaultDisableDuration.collect { duration ->
                _uiState.value = _uiState.value.copy(defaultDisableDuration = duration)
            }
        }
    }

    /** Poll the accessibility service running state every second (no broadcast mechanism needed). */
    private fun pollServiceRunningState() {
        viewModelScope.launch {
            while (true) {
                val running = XBlockerAccessibilityService.isRunning(context)
                if (_uiState.value.isAccessibilityServiceRunning != running) {
                    _uiState.value = _uiState.value.copy(isAccessibilityServiceRunning = running)
                    // Sync preferences with actual service state
                    if (!running) {
                        preferences.setBlockerActive(false)
                    }
                }
                delay(1000)
            }
        }
    }

    /** Ticks every second to update countdown display */
    private fun tickTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val timer = _uiState.value.activeDisableTimer
                if (timer != null) {
                    _uiState.value = _uiState.value.copy(timerRemainingMs = timer.remainingMs())
                }
            }
        }
    }

    /** Open system Accessibility Settings so the user can enable XBlocker. */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Disable blocker using the default duration from settings. */
    fun requestDisable() {
        viewModelScope.launch {
            requestDisableBlocker(_uiState.value.defaultDisableDuration)
        }
    }

    fun cancelDisableTimer() {
        viewModelScope.launch {
            val timer = _uiState.value.activeDisableTimer ?: return@launch
            cancelTimer(timer.id)
        }
    }
}
