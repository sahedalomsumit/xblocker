package com.sahed.xblocker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TimerEventEntity): Long

    @Update
    suspend fun update(event: TimerEventEntity)

    @Query("SELECT * FROM timer_events WHERE isComplete = 0 AND cancelled = 0 ORDER BY triggeredAt DESC")
    fun getPendingEvents(): Flow<List<TimerEventEntity>>

    @Query("SELECT * FROM timer_events WHERE isComplete = 0 AND cancelled = 0 ORDER BY triggeredAt DESC")
    suspend fun getPendingEventsOnce(): List<TimerEventEntity>

    @Query("SELECT * FROM timer_events WHERE eventType = 'DISABLE_BLOCKER' AND isComplete = 0 AND cancelled = 0 LIMIT 1")
    fun getActiveDisableTimer(): Flow<TimerEventEntity?>

    @Query("SELECT * FROM timer_events WHERE domainId = :domainId AND isComplete = 0 AND cancelled = 0 LIMIT 1")
    fun getActiveTimerForDomain(domainId: Long): Flow<TimerEventEntity?>

    @Query("UPDATE timer_events SET cancelled = 1 WHERE id = :id")
    suspend fun cancel(id: Long)

    @Query("UPDATE timer_events SET isComplete = 1 WHERE id = :id")
    suspend fun markComplete(id: Long)

    @Query("UPDATE timer_events SET cancelled = 1 WHERE eventType = 'DISABLE_BLOCKER' AND isComplete = 0")
    suspend fun cancelAllDisableTimers()

    @Query("SELECT * FROM timer_events WHERE id = :id")
    suspend fun getById(id: Long): TimerEventEntity?
}
