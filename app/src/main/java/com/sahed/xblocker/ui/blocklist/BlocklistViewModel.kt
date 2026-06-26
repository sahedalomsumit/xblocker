package com.sahed.xblocker.ui.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahed.xblocker.data.repository.BlocklistRepository
import com.sahed.xblocker.domain.model.BlockedDomain
import com.sahed.xblocker.domain.usecase.AddCustomDomainUseCase
import com.sahed.xblocker.domain.usecase.CancelTimerUseCase
import com.sahed.xblocker.domain.usecase.RequestDomainRemovalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BlocklistFilter { ALL, CUSTOM, PENDING }

data class BlocklistUiState(
    val domains: List<BlockedDomain> = emptyList(),
    val filter: BlocklistFilter = BlocklistFilter.ALL,
    val inputText: String = "",
    val inputError: String? = null,
    val isAdding: Boolean = false
) {
    val filteredDomains get() = when (filter) {
        BlocklistFilter.ALL -> domains
        BlocklistFilter.CUSTOM -> domains.filter { it.isCustom && !it.pendingDelete }
        BlocklistFilter.PENDING -> domains.filter { it.pendingDelete }
    }
}

@HiltViewModel
class BlocklistViewModel @Inject constructor(
    private val repo: BlocklistRepository,
    private val addDomain: AddCustomDomainUseCase,
    private val requestRemoval: RequestDomainRemovalUseCase,
    private val cancelTimer: CancelTimerUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BlocklistUiState())
    val state: StateFlow<BlocklistUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getCustomDomains().collect { domains ->
                _state.value = _state.value.copy(domains = domains)
            }
        }
    }

    fun onInputChange(text: String) {
        _state.value = _state.value.copy(inputText = text, inputError = null)
    }

    fun onFilterChange(filter: BlocklistFilter) {
        _state.value = _state.value.copy(filter = filter)
    }

    fun addDomain() {
        val input = _state.value.inputText.trim()
        if (input.isBlank()) {
            _state.value = _state.value.copy(inputError = "Please enter a domain")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isAdding = true)
            when (val result = addDomain(input)) {
                is AddCustomDomainUseCase.Result.Success -> {
                    _state.value = _state.value.copy(inputText = "", inputError = null, isAdding = false)
                }
                is AddCustomDomainUseCase.Result.EmptyDomain ->
                    _state.value = _state.value.copy(inputError = "Domain cannot be empty", isAdding = false)
                is AddCustomDomainUseCase.Result.InvalidDomain ->
                    _state.value = _state.value.copy(inputError = "Invalid domain format (e.g. example.com)", isAdding = false)
                is AddCustomDomainUseCase.Result.AlreadyExists ->
                    _state.value = _state.value.copy(inputError = "Domain already in blocklist", isAdding = false)
            }
        }
    }

    fun requestDelete(domain: BlockedDomain) {
        viewModelScope.launch {
            requestRemoval(domain.id)
        }
    }

    fun cancelDelete(domain: BlockedDomain) {
        viewModelScope.launch {
            cancelTimer.cancelByDomain(domain.id)
        }
    }
}
