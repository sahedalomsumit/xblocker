package com.sahed.xblocker.domain.model

data class BlockedDomain(
    val id: Long = 0,
    val domain: String,
    val isCustom: Boolean,
    val addedAt: Long = System.currentTimeMillis(),
    val pendingDelete: Boolean = false,
    val deleteAt: Long? = null,
    val blockedCount: Int = 0
) {
    /** Remaining milliseconds until deletion executes */
    fun deleteRemainingMs(): Long? {
        if (!pendingDelete || deleteAt == null) return null
        return (deleteAt - System.currentTimeMillis()).coerceAtLeast(0)
    }
}
