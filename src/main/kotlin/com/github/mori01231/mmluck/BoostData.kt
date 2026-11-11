package com.github.mori01231.mmluck

/**
 * @property startTime start time in epoch milliseconds
 * @property duration duration in seconds
 * @property percentage boost percentage
 */
data class BoostData(
    val startTime: Long,
    val duration: Long,
    val percentage: Long,
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now - startTime > duration * 1000

    /** Remaining time in seconds. */
    fun remainingSeconds(now: Long = System.currentTimeMillis()): Long = (startTime + duration * 1000 - now) / 1000
}
