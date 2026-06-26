package com.sahed.xblocker.domain.usecase

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sahed.xblocker.data.repository.BlocklistRepository
import com.sahed.xblocker.data.repository.TimerRepository
import com.sahed.xblocker.service.TimerWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RequestDomainRemovalUseCase @Inject constructor(
    private val blocklistRepo: BlocklistRepository,
    private val timerRepo: TimerRepository,
    private val workManager: WorkManager
) {
    suspend operator fun invoke(domainId: Long, delayMs: Long = TimerRepository.THREE_HOURS_MS) {
        if (delayMs == TimerRepository.FOREVER_MS) {
            timerRepo.createRemoveDomainTimer(domainId = domainId, workerId = "forever", delayMs = delayMs)
            blocklistRepo.markPendingDelete(domainId, System.currentTimeMillis() + delayMs)
            return
        }

        val deleteAt = System.currentTimeMillis() + delayMs

        val workRequest = OneTimeWorkRequestBuilder<TimerWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    TimerWorker.KEY_EVENT_TYPE to "REMOVE_DOMAIN",
                    TimerWorker.KEY_DOMAIN_ID to domainId
                )
            )
            .addTag(TimerWorker.TAG_REMOVE_DOMAIN)
            .build()

        workManager.enqueueUniqueWork(
            "${TimerWorker.WORK_REMOVE_DOMAIN}_$domainId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        blocklistRepo.markPendingDelete(domainId, deleteAt)
        timerRepo.createRemoveDomainTimer(domainId, workRequest.id.toString(), delayMs)
    }
}
