package com.sahed.xblocker.domain.usecase

import com.sahed.xblocker.data.repository.TimerRepository
import com.sahed.xblocker.domain.model.TimerEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPendingTimersUseCase @Inject constructor(
    private val timerRepo: TimerRepository
) {
    operator fun invoke(): Flow<List<TimerEvent>> = timerRepo.getPendingEvents()
}
