package dev.darkblade.croppy.platform.paper.station.display;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class VirtualEntityIdGeneratorTest {

    @BeforeEach
    void setup() {
        VirtualEntityIdGenerator.reset();
    }

    @Test
    void testNegativeIdsGenerated() {
        int id1 = VirtualEntityIdGenerator.nextEntityId();
        int id2 = VirtualEntityIdGenerator.nextEntityId();

        assertTrue(id1 < 0, "ID should be negative");
        assertTrue(id2 < 0, "ID should be negative");
        assertNotEquals(id1, id2, "IDs should be distinct");
        assertTrue(id2 < id1, "IDs should decrement");
    }

    @Test
    void testConcurrentUniqueIds() throws InterruptedException {
        int threads = 10;
        int idsPerThread = 500;
        int totalExpected = threads * idsPerThread;

        Set<Integer> generatedIds = Collections.synchronizedSet(new HashSet<>());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        generatedIds.add(VirtualEntityIdGenerator.nextEntityId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(totalExpected, generatedIds.size(), "All generated IDs must be unique across threads");
        for (int id : generatedIds) {
            assertTrue(id < 0, "Every ID must be negative");
        }
    }
}
