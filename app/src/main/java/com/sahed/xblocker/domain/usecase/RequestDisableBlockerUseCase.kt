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
    suspend operator fun invoke() {
        val workRequest = OneTimeWorkRequestBuilder<TimerWorker>()
            .setInitialDelay(TimerRepository.THREE_HOURS_MS, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(TimerWorker.KEY_EVENT_TYPE to "DISABLE_BLOCKER"))
            .addTag(TimerWorker.TAG_DISABLE)
            .build()

        workManager.enqueueUniqueWork(
            TimerWorker.WORK_DISABLE_BLOCKER,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        timerRepo.createDisableBlockerTimer(workRequest.id.toString())
    }
}
