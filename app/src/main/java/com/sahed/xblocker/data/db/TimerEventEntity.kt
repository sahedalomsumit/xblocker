package com.sahed.xblocker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timer_events",
    foreignKeys = [
        ForeignKey(
            entity = BlockedDomainEntity::class,
            parentColumns = ["id"],
            childColumns = ["domainId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("domainId")]
)
data class TimerEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String, // "DISABLE_BLOCKER" | "REMOVE_DOMAIN"
    val triggeredAt: Long = System.currentTimeMillis(),
    val executeAt: Long, // triggeredAt + 10_800_000 (3 hours)
    val domainId: Long? = null,
    val isComplete: Boolean = false,
    val cancelled: Boolean = false,
    val workerId: String? = null // WorkManager request ID
)
