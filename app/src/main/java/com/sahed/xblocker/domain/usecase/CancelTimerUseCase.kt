package com.sahed.xblocker.domain.usecase

import androidx.work.WorkManager
import com.sahed.xblocker.data.repository.BlocklistRepository
import com.sahed.xblocker.data.repository.TimerRepository
import com.sahed.xblocker.domain.model.TimerEventType
import javax.inject.Inject

class CancelTimerUseCase @Inject constructor(
    private val timerRepo: TimerRepository,
    private val blocklistRepo: BlocklistRepository,
    private val workManager: WorkManager
) {
    suspend operator fun invoke(timerId: Long) {
        val event = timerRepo.getById(timerId) ?: return
        timerRepo.cancel(timerId)

        // Cancel the WorkManager job if we stored the worker ID
        event.workerId?.let { workManager.cancelWorkById(java.util.UUID.fromString(it)) }

        // If it was a domain removal timer, un-mark the domain
        if (event.eventType == TimerEventType.REMOVE_DOMAIN && event.domainId != null) {
            blocklistRepo.cancelPendingDelete(event.domainId)
        }
    }
}
