package com.imsproject.watch.utils

data class LatencyStatistics(
    val averageLatency: Double,
    val minLatency: Double,
    val maxLatency: Double,
    val jitter: Double,
    val median: Double,
    val measurementCount: Int,
    val timeoutThreshold: Int,
    val timeoutsCount: Int
) {
    companion object {
        fun empty(): LatencyStatistics = LatencyStatistics(
            averageLatency = -1.0,
            minLatency = -1.0,
            maxLatency = -1.0,
            jitter = -1.0,
            median = -1.0,
            measurementCount = 0,
            timeoutThreshold = -1,
            timeoutsCount = 0
        )
    }
}
