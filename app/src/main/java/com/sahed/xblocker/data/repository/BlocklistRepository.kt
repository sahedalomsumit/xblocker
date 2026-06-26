package com.sahed.xblocker.data.repository

import com.sahed.xblocker.data.db.BlockedDomainEntity
import com.sahed.xblocker.data.db.BlocklistDao
import com.sahed.xblocker.domain.model.BlockedDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistRepository @Inject constructor(
    private val dao: BlocklistDao
) {

    fun getCustomDomains(): Flow<List<BlockedDomain>> =
        dao.getCustomDomains().map { list -> list.map { it.toDomain() } }

    fun getAllDomains(): Flow<List<BlockedDomain>> =
        dao.getAllDomains().map { list -> list.map { it.toDomain() } }

    suspend fun getAllActiveDomains(): List<String> = dao.getAllActiveDomains()

    fun getActiveCustomCount(): Flow<Int> = dao.getActiveCustomCount()

    fun getTotalBlockedCount(): Flow<Int?> = dao.getTotalBlockedCount()

    suspend fun addDomain(domain: String): Long {
        val entity = BlockedDomainEntity(
            domain = domain.lowercase().trim(),
            isCustom = true
        )
        return dao.insert(entity)
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun getById(id: Long): BlockedDomain? = dao.getById(id)?.toDomain()

    suspend fun markPendingDelete(id: Long, deleteAt: Long) =
        dao.markPendingDelete(id, deleteAt)

    suspend fun cancelPendingDelete(id: Long) = dao.cancelPendingDelete(id)

    suspend fun incrementBlockedCount(domain: String) = dao.incrementBlockedCount(domain)

    private fun BlockedDomainEntity.toDomain() = BlockedDomain(
        id = id,
        domain = domain,
        isCustom = isCustom,
        addedAt = addedAt,
        pendingDelete = pendingDelete,
        deleteAt = deleteAt,
        blockedCount = blockedCount
    )
}
