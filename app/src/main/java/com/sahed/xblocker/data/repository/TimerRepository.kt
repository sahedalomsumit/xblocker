package com.sahed.xblocker.data.repository

import com.sahed.xblocker.data.db.TimerDao
import com.sahed.xblocker.data.db.TimerEventEntity
import com.sahed.xblocker.domain.model.TimerEvent
import com.sahed.xblocker.domain.model.TimerEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val dao: TimerDao
) {

    fun getPendingEvents(): Flow<List<TimerEvent>> =
        dao.getPendingEvents().map { it.map { e -> e.toDomain() } }

    suspend fun getPendingEventsOnce(): List<TimerEvent> =
        dao.getPendingEventsOnce().map { it.toDomain() }

    fun getActiveDisableTimer(): Flow<TimerEvent?> =
        dao.getActiveDisableTimer().map { it?.toDomain() }

    fun getActiveTimerForDomain(domainId: Long): Flow<TimerEvent?> =
        dao.getActiveTimerForDomain(domainId).map { it?.toDomain() }

    suspend fun createDisableBlockerTimer(workerId: String): Long {
        val now = System.currentTimeMillis()
        val event = TimerEventEntity(
            eventType = TimerEventType.DISABLE_BLOCKER.name,
            triggeredAt = now,
            executeAt = now + THREE_HOURS_MS,
            workerId = workerId
        )
        return dao.insert(event)
    }

    suspend fun createRemoveDomainTimer(domainId: Long, workerId: String): Long {
        val now = System.currentTimeMillis()
        val event = TimerEventEntity(
            eventType = TimerEventType.REMOVE_DOMAIN.name,
            triggeredAt = now,
            executeAt = now + THREE_HOURS_MS,
            domainId = domainId,
            workerId = workerId
        )
        return dao.insert(event)
    }

    suspend fun cancel(id: Long) = dao.cancel(id)

    suspend fun markComplete(id: Long) = dao.markComplete(id)

    suspend fun cancelAllDisableTimers() = dao.cancelAllDisableTimers()

    suspend fun getById(id: Long): TimerEvent? = dao.getById(id)?.toDomain()

    private fun TimerEventEntity.toDomain() = TimerEvent(
        id = id,
        eventType = TimerEventType.valueOf(eventType),
        triggeredAt = triggeredAt,
        executeAt = executeAt,
        domainId = domainId,
        isComplete = isComplete,
        cancelled = cancelled,
        workerId = workerId
    )

    companion object {
        const val THREE_HOURS_MS = 3 * 60 * 60 * 1000L
    }
}
