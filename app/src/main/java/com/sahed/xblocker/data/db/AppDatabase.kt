package com.sahed.xblocker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BlockedDomainEntity::class, TimerEventEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blocklistDao(): BlocklistDao
    abstract fun timerDao(): TimerDao

    companion object {
        const val DATABASE_NAME = "xblocker.db"
    }
}
