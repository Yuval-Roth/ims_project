package com.imsproject.watch.utils

import com.imsproject.common.networking.UdpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.SocketTimeoutException
import kotlin.math.abs

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
    private var currentTimeoutThreshold: Int = MIN_TIMEOUT_MS
    private val dataLock = Mutex(false)

    var onReceive: (Double) -> Unit = {}
    var onTimeout: () -> Unit = {}

    fun start() {
        if (job != null) return

        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val sentTime = System.nanoTime()
                    udpClient.send(pingMessage)

                    try {
                        udpClient.receive()
                    } catch (_: SocketTimeoutException) {
                        dataLock.withLock {
                            timeoutsCount++
                        }
                        continue
                    }

                    val latency = (System.nanoTime() - sentTime) / 1_000_000.0
                    onReceive(latency)

                    // Update statistics
                    dataLock.withLock {
                        lastLatency?.let { jitterSum += abs(latency - it) }
                        lastLatency = latency
                        latencies.add(latency)
                    }

                    // try to get 20 samples per second
                    delay(48 - (System.nanoTime() - sentTime) / 1_000_000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            udpClient.close()
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
    private fun adjustTimeoutThreshold(latencies: List<Double>, averageLatency: Double) {
        if (latencies.isEmpty()) return

        val adjustedMax = computePercentile(latencies, 95)
        val desiredTimeout = (averageLatency * 2)
            .coerceAtLeast(adjustedMax)
            .coerceAtLeast(MIN_TIMEOUT_MS.toDouble())

        val smoothedTimeoutThreshold = (currentTimeoutThreshold * SMOOTHING_FACTOR + desiredTimeout * (1 - SMOOTHING_FACTOR)).toInt()

        udpClient.setTimeout(smoothedTimeoutThreshold)
        currentTimeoutThreshold = smoothedTimeoutThreshold
    }

    /**
     * Computes the Nth percentile from a list of latencies.
     * @param sortedLatencies The list of latency measurements.
     * @param percentile The desired percentile (0-100).
     * @return The latency at the specified percentile or -1 if the list is empty.
     */
    private fun computePercentile(sortedLatencies: List<Double>, percentile: Int): Double {
        if (sortedLatencies.isEmpty()) return -1.0

        val index = (percentile / 100.0 * (sortedLatencies.size - 1)).toInt()
        return sortedLatencies[index]
    }

    /**
     * Calculates the median of the latencies from the sorted list.
     */
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
}