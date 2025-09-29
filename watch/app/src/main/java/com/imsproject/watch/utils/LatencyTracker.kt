package com.imsproject.watch.utils

import android.os.SystemClock
import com.imsproject.common.networking.UdpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.SocketTimeoutException
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

class LatencyTracker(
    private val scope: CoroutineScope,
    private val pingMessage: String,
    remoteAddress: String,
    remotePort: Int,
    initialTimeout: Int = 500
) {
    private val udpClient = UdpClient().apply {
        this.remotePort = remotePort
        this.remoteAddress = remoteAddress
        init()
        setTimeout(initialTimeout)
    }

    // Constants for minimum timeout and smoothing
    private companion object {
        const val MIN_TIMEOUT_MS = 25  // Minimum timeout in milliseconds
        const val SMOOTHING_FACTOR = 0.8  // Factor for smoothing adjustments (closer to 1 = less change)
    }

    private var job: Job? = null

    // Statistics fields
    private var latencies = mutableListOf<Double>()
    private var lastLatency: Double? = null
    private var jitterSum = 0.0
    private var timeoutsCount = 0
    private var currentTimeoutThreshold = initialTimeout
    private val dataLock = Mutex(false)

    var onReceive: (Double) -> Unit = {}
    var onTimeout: (Double) -> Unit = {}

    fun start() {
        if (job != null) return

        job = scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val sentTime = SystemClock.elapsedRealtimeNanos()
                    udpClient.send(pingMessage)

                    try {
                        udpClient.receive()
                    } catch (_: SocketTimeoutException) {
                        val latency = (SystemClock.elapsedRealtimeNanos() - sentTime) / 1_000_000.0
                        onTimeout(latency)
                        dataLock.withLock {
                            timeoutsCount++
                            lastLatency?.let { jitterSum += abs(latency - it) }
                            lastLatency = latency
                            latencies.add(latency)
                        }
                        continue
                    }
                    val latency = (SystemClock.elapsedRealtimeNanos() - sentTime) / 1_000_000.0
                    onReceive(latency)

                    // Update statistics
                    dataLock.withLock {
                        lastLatency?.let { jitterSum += abs(latency - it) }
                        lastLatency = latency
                        latencies.add(latency)
                    }

                    // try to get 20 samples per second
                    delay(45 - (SystemClock.elapsedRealtimeNanos() - sentTime) / 1_000_000)
                }
            } finally {
                udpClient.close()
            }
        }
    }

    /**
     * Returns the latency statistics since the last call and resets all collected data.
     */
    suspend fun collectStatistics(): LatencyStatistics {
        // Save the current state and start collecting new data
        val oldLatencies = latencies
        val oldJitterSum: Double
        val oldTimeoutsCount : Int
        dataLock.withLock {
            oldJitterSum = jitterSum
            oldTimeoutsCount = timeoutsCount
            latencies = mutableListOf()
            lastLatency = null
            jitterSum = 0.0
            timeoutsCount = 0
        }

        // process the old data
        oldLatencies.sort()
        val stats = extractStatistics(oldLatencies,oldJitterSum,oldTimeoutsCount)
        adjustTimeoutThreshold(oldLatencies, stats.averageLatency)

        return stats
    }

    /**
     * Returns the current latency statistics without resetting the collected data.
     */
    private fun extractStatistics(
        sortedLatencies: List<Double>,
        jitterSum: Double,
        timeoutsCount: Int
    ): LatencyStatistics {
        if (sortedLatencies.isEmpty()) return LatencyStatistics.empty()

        val averageLatency = sortedLatencies.average()
        val minLatency = sortedLatencies.first()
        val maxLatency = sortedLatencies.last()
        val jitter = if (sortedLatencies.size < 2) -1.0 else jitterSum / (sortedLatencies.size - 1)
        val median = computeMedian(sortedLatencies)
        val measurementCount = sortedLatencies.size

        return LatencyStatistics(
            averageLatency,
            minLatency,
            maxLatency,
            jitter,
            median,
            measurementCount,
            currentTimeoutThreshold,
            timeoutsCount
        )
    }

    /**
     * Adjusts the UDP client's timeout dynamically based on the current latency data.
     */
    private fun adjustTimeoutThreshold(sortedLatencies: List<Double>, averageLatency: Double) {
        if (sortedLatencies.isEmpty()) return
        val standardDeviation = computeStandardDeviation(sortedLatencies, averageLatency)
        val desiredTimeout = ceil(averageLatency + standardDeviation * 4).toInt()
        val coercedTimeout = desiredTimeout.fastCoerceAtLeast(MIN_TIMEOUT_MS)
        val smoothedTimeoutThreshold = (currentTimeoutThreshold * SMOOTHING_FACTOR + coercedTimeout * (1 - SMOOTHING_FACTOR)).toInt()
        udpClient.setTimeout(smoothedTimeoutThreshold)
        currentTimeoutThreshold = smoothedTimeoutThreshold
    }

    private fun computeMedian(sortedLatencies: List<Double>): Double {
        val size = sortedLatencies.size
        return if (size == 0) {
            -1.0
        } else if (size % 2 == 0) {
            // Average of the two middle values for even number of latencies
            (sortedLatencies[size / 2 - 1] + sortedLatencies[size / 2]) / 2.0
        } else {
            // Middle value for odd number of latencies
            sortedLatencies[size / 2]
        }
    }

    private fun computeStandardDeviation(latencies: List<Double>, mean: Double): Double {
        val variance = latencies.sumOf { (it - mean).pow(2) } / latencies.size
        return sqrt(variance)
    }
}