package in.proofofconcept.url.shortner.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Distributed 64-bit ID generator based on Twitter Snowflake.
 * Structure (64 bits):
 * - 1 bit: Unused sign bit (always 0)
 * - 41 bits: Timestamp in milliseconds since custom epoch (~69 years)
 * - 5 bits: Datacenter ID (0 - 31)
 * - 5 bits: Worker ID (0 - 31)
 * - 12 bits: Sequence number (0 - 4095 per ms)
 */
@Component
public class SnowflakeIdGenerator {

    // Custom epoch: 2024-01-01T00:00:00Z
    private static final long EPOCH = 1704067200000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    private static final long SEQUENCE_BITS = 12L;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long datacenterId;
    private final long workerId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(
            @Value("${snowflake.datacenter-id:1}") long datacenterId,
            @Value("${snowflake.worker-id:1}") long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("Worker ID can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("Datacenter ID can't be greater than %d or less than 0", MAX_DATACENTER_ID));
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long currentTimestamp = timeGen();

        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;
            if (offset <= 5) {
                // Minor clock drift, spin wait
                try {
                    wait(offset << 1);
                    currentTimestamp = timeGen();
                    if (currentTimestamp < lastTimestamp) {
                        throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + offset + " milliseconds");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + offset + " milliseconds");
            }
        }

        if (lastTimestamp == currentTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // Sequence exhausted for this millisecond, wait for next ms
                currentTimestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }
}
