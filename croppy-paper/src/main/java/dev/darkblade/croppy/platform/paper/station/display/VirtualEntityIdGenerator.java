package dev.darkblade.croppy.platform.paper.station.display;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe generator providing negative entity IDs for virtual client-side entities.
 * <p>
 * Using negative integer values ensures virtual entity IDs never collide with vanilla
 * Minecraft or Paper server-side entity IDs, which are allocated sequentially as positive integers.
 */
public final class VirtualEntityIdGenerator {

    private static final int INITIAL_ID = -1000;
    private static final AtomicInteger COUNTER = new AtomicInteger(INITIAL_ID);

    private VirtualEntityIdGenerator() {}

    /**
     * Allocates the next distinct negative entity ID.
     *
     * @return unique negative integer entity ID
     */
    public static int nextEntityId() {
        int id = COUNTER.decrementAndGet();
        // Safeguard against underflow: wrap back to INITIAL_ID
        if (id > 0) {
            COUNTER.compareAndSet(id, INITIAL_ID);
            return COUNTER.decrementAndGet();
        }
        return id;
    }

    /**
     * Resets the counter back to default initial seed (primarily for testing).
     */
    public static void reset() {
        COUNTER.set(INITIAL_ID);
    }
}
