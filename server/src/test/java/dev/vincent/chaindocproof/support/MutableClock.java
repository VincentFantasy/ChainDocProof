package dev.vincent.chaindocproof.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 可推进的测试时钟，用于冷却/锁定等时间边界测试
 */
public final class MutableClock extends Clock {

    private final AtomicLong millis = new AtomicLong(0);

    /** 设置当前时间 */
    public void set(long epochMillis) {
        millis.set(epochMillis);
    }

    /** 向后推进 */
    public void advance(long ms) {
        millis.addAndGet(ms);
    }

    public long millis() {
        return millis.get();
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(millis.get());
    }
}
