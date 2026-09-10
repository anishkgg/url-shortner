package in.proofofconcept.url.shortner.util;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {

    @Test
    void testNextIdIsPositiveAndMonotonic() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        long id3 = generator.nextId();

        assertTrue(id1 > 0);
        assertTrue(id2 > id1);
        assertTrue(id3 > id2);
    }

    @Test
    void testConcurrentUniqueIdGeneration() throws InterruptedException {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(2, 2);
        int threadCount = 20;
        int idsPerThread = 500;
        int totalExpected = threadCount * idsPerThread;

        Set<Long> generatedIds = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        generatedIds.add(generator.nextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(totalExpected, generatedIds.size(), "All generated IDs across concurrent threads must be strictly unique");
    }

    @Test
    void testInvalidWorkerOrDatacenterId() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1, -1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(32, 1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1, 32));
    }
}
