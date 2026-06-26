package com.sahed.xblocker.domain.model

enum class TimerEventType {
    DISABLE_BLOCKER,
    REMOVE_DOMAIN
}

data class TimerEvent(
    val id: Long = 0,
    val eventType: TimerEventType,
    val triggeredAt: Long,
    val executeAt: Long,
    val domainId: Long? = null,
    val isComplete: Boolean = false,
    val cancelled: Boolean = false,
    val workerId: String? = null
) {
    fun remainingMs(): Long = (executeAt - System.currentTimeMillis()).coerceAtLeast(0)
    fun isPending(): Boolean = !isComplete && !cancelled
}
