package com.imsproject.common.utils

import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * A utility class for generating short unique identifiers.
 * The identifiers are based on a counter and are converted to a UUID string format.
 * The actual maximum length of the generated identifier is 32 characters (the length of a UUID string without hyphens).
 */
class SimpleIdGenerator(length: Int) {
    private val counter: AtomicLong
    private val length: Int

    /**
     * Constructs a SimpleIdGenerator with the specified length.
     *
     * @param length the length of the generated identifier (1-32)
     */
    init {
        require(!(length < 1 || length > 32)) { "The length must be between 1 and 32." }
        this.length = length
        counter = AtomicLong()
    }

    /**
     * Generates a new unique identifier.
     *
     * @return a unique identifier string of the specified length
     */
    fun generate(): String {
        return UUID(0, counter.incrementAndGet())
            .toString()
            .replace("-", "")
            .substring(32 - length)
    }
}