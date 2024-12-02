package com.imsproject.common.utils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A utility class for generating short unique identifiers.
 * The identifiers are based on a counter and are converted to a UUID string format.
 * The actual maximum length of the generated identifier is 32 characters (the length of a UUID string without hyphens).
 */
public class SimpleIdGenerator {
    private final AtomicLong counter;
    private final int length;

    /**
     * Constructs a SimpleIdGenerator with the specified length.
     *
     * @param length the length of the generated identifier (1-32)
     */
    public SimpleIdGenerator(int length) {
        if(length < 1 || length > 32)
            throw new IllegalArgumentException("The length must be between 1 and 32.");
        this.length = length;
        counter = new AtomicLong();
    }

    /**
     * Generates a new unique identifier.
     *
     * @return a unique identifier string of the specified length
     */
    public String generate() {
        return new UUID(0, counter.incrementAndGet())
                .toString()
                .replace("-", "")
                .substring(32 - length);
    }
}