package com.tap.apk.detection

import com.tap.apk.models.TapEvent

class MultiTapDetector(
    private val minTapGapMs: Long = 100,
    private val singleTimeoutMs: Long = 320,
    private val doubleWindowMs: Long = 400,
    private val tripleWindowMs: Long = 600,
) {
    private val peaks = ArrayDeque<Long>()

    fun onTapPeak(timestampMs: Long): TapEvent? {
        if (peaks.isNotEmpty() && timestampMs - peaks.last() < minTapGapMs) {
            return null
        }
        peaks.addLast(timestampMs)
        prune(timestampMs)
        return null
    }

    fun flush(nowMs: Long): TapEvent? {
        if (peaks.isEmpty()) return null
        val first = peaks.first()
        val count = peaks.size
        val elapsed = nowMs - first

        val event = when {
            count >= 3 && elapsed <= tripleWindowMs -> TapEvent.Triple
            count == 2 && elapsed <= doubleWindowMs && nowMs - peaks.last() >= minTapGapMs -> TapEvent.Double
            count == 1 && nowMs - peaks.last() >= singleTimeoutMs -> TapEvent.Single
            elapsed > tripleWindowMs -> when {
                count >= 3 -> TapEvent.Triple
                count == 2 -> TapEvent.Double
                else -> TapEvent.Single
            }
            else -> null
        }

        if (event != null) {
            peaks.clear()
        }
        return event
    }

    private fun prune(nowMs: Long) {
        while (peaks.isNotEmpty() && nowMs - peaks.first() > tripleWindowMs) {
            peaks.removeFirst()
        }
    }
}
