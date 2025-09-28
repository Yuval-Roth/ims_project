package com.imsproject.watch.utils

class FlingTracker {
    private var startX = 0f
    private var startY = 0f
    private var startTime = 0L

    fun startFling(x: Float, y: Float) {
        startX = x
        startY = y
        startTime = System.nanoTime()
    }

    /**
     *  @return (normalizedX, normalizedY, velocity)
     */
    fun endFling(x: Float, y: Float): Triple<Float, Float, Float> {
        val endTime = System.nanoTime()
        val deltaTime = (endTime - startTime) / 1_000_000_000f // Convert to seconds
        val deltaX = x - startX
        val deltaY = y - startY
        val distance = kotlin.math.hypot(deltaX, deltaY)
        val velocity = if (deltaTime > 0) distance / deltaTime else 0f // pixels per second
        val normalizedX = if (distance != 0f) deltaX / distance else 0f
        val normalizedY = if (distance != 0f) deltaY / distance else 0f
        return Triple(normalizedX, normalizedY, velocity)
    }
}