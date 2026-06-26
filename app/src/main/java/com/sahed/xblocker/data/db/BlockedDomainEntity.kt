package com.sahed.xblocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_domains")
data class BlockedDomainEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val isCustom: Boolean,
    val addedAt: Long = System.currentTimeMillis(),
    val pendingDelete: Boolean = false,
    val deleteAt: Long? = null,
    val blockedCount: Int = 0
)
