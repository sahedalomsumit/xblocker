package com.sahed.xblocker.domain.usecase

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sahed.xblocker.data.repository.TimerRepository
import com.sahed.xblocker.service.TimerWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RequestDisableBlockerUseCase @Inject constructor(
    private val timerRepo: TimerRepository,
    private val workManager: WorkManager
) {
    /**
     * Schedules a blocker-disable event after [delayMs] milliseconds.
     * If [delayMs] == [TimerRepository.FOREVER_MS], no WorkManager job is
     * enqueued — the blocker stays off indefinitely until the timer is cancelled.
     */
    suspend operator fun invoke(delayMs: Long = TimerRepository.THREE_HOURS_MS) {
        if (delayMs == TimerRepository.FOREVER_MS) {
            // "Forever" — record the intent in DB but don't schedule a job
            timerRepo.createDisableBlockerTimer(workerId = "forever", delayMs = delayMs)
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<TimerWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(TimerWorker.KEY_EVENT_TYPE to "DISABLE_BLOCKER"))
            .addTag(TimerWorker.TAG_DISABLE)
            .build()

        workManager.enqueueUniqueWork(
            TimerWorker.WORK_DISABLE_BLOCKER,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        timerRepo.createDisableBlockerTimer(workRequest.id.toString(), delayMs)
    }
}
