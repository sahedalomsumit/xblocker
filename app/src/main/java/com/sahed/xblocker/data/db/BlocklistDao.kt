package com.sahed.xblocker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BlocklistDao {

    @Query("SELECT * FROM blocked_domains WHERE isCustom = 1 ORDER BY addedAt DESC")
    fun getCustomDomains(): Flow<List<BlockedDomainEntity>>

    @Query("SELECT * FROM blocked_domains ORDER BY addedAt DESC")
    fun getAllDomains(): Flow<List<BlockedDomainEntity>>

    @Query("SELECT domain FROM blocked_domains WHERE pendingDelete = 0")
    suspend fun getAllActiveDomains(): List<String>

    @Query("SELECT COUNT(*) FROM blocked_domains WHERE isCustom = 1 AND pendingDelete = 0")
    fun getActiveCustomCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(domain: BlockedDomainEntity): Long

    @Update
    suspend fun update(domain: BlockedDomainEntity)

    @Delete
    suspend fun delete(domain: BlockedDomainEntity)

    @Query("DELETE FROM blocked_domains WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM blocked_domains WHERE id = :id")
    suspend fun getById(id: Long): BlockedDomainEntity?

    @Query("UPDATE blocked_domains SET blockedCount = blockedCount + 1 WHERE domain = :domain")
    suspend fun incrementBlockedCount(domain: String)

    @Query("SELECT SUM(blockedCount) FROM blocked_domains")
    fun getTotalBlockedCount(): Flow<Int?>

    @Query("UPDATE blocked_domains SET pendingDelete = 1, deleteAt = :deleteAt WHERE id = :id")
    suspend fun markPendingDelete(id: Long, deleteAt: Long)

    @Query("UPDATE blocked_domains SET pendingDelete = 0, deleteAt = NULL WHERE id = :id")
    suspend fun cancelPendingDelete(id: Long)
}
