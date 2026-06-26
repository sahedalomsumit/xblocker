package com.sahed.xblocker.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahed.xblocker.data.datastore.AppPreferences
import com.sahed.xblocker.data.repository.BlocklistRepository
import com.sahed.xblocker.data.repository.TimerRepository
import com.sahed.xblocker.domain.model.TimerEvent
import com.sahed.xblocker.domain.usecase.CancelTimerUseCase
import com.sahed.xblocker.domain.usecase.RequestDisableBlockerUseCase
import com.sahed.xblocker.service.XBlockerVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isVpnActive: Boolean = false,
    val blockedToday: Int = 0,
    val customDomainCount: Int = 0,
    val activeDisableTimer: TimerEvent? = null,
    val timerRemainingMs: Long = 0L
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
    }

    private fun observeState() {
        viewModelScope.launch {
            preferences.isVpnActive.collect { active ->
                _uiState.value = _uiState.value.copy(isVpnActive = active)
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
            timerRepo.getActiveDisableTimer().collect { timer ->
                _uiState.value = _uiState.value.copy(
                    activeDisableTimer = timer,
                    timerRemainingMs = timer?.remainingMs() ?: 0L
                )
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

    fun startVpn() {
        val intent = Intent(context, XBlockerVpnService::class.java).apply {
            action = XBlockerVpnService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun requestDisable() {
        viewModelScope.launch {
            requestDisableBlocker()
        }
    }

    fun cancelDisableTimer() {
        viewModelScope.launch {
            val timer = _uiState.value.activeDisableTimer ?: return@launch
            cancelTimer(timer.id)
        }
    }
}
