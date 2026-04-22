package com.dxd.oj.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class JudgeConcurrencyLimiter {

    private static final String CONFIG_JUDGE_MAX_CONCURRENCY = "judge.max_concurrency";
    private static final int DEFAULT_MAX_CONCURRENCY = 2;
    private static final int MIN_MAX_CONCURRENCY = 1;
    private static final int MAX_MAX_CONCURRENCY = 64;

    private final SystemConfigAccessor systemConfigAccessor;
    private final Object monitor = new Object();
    private int running = 0;

    public Permit acquirePermit() throws InterruptedException {
        while (true) {
            int limit = currentLimit();
            synchronized (monitor) {
                if (running < limit) {
                    running++;
                    return new Permit(this);
                }
                monitor.wait(200L);
            }
        }
    }

    private void releasePermit() {
        synchronized (monitor) {
            if (running > 0) {
                running--;
            }
            monitor.notifyAll();
        }
    }

    private int currentLimit() {
        return systemConfigAccessor.getInt(
                CONFIG_JUDGE_MAX_CONCURRENCY,
                DEFAULT_MAX_CONCURRENCY,
                MIN_MAX_CONCURRENCY,
                MAX_MAX_CONCURRENCY
        );
    }

    public static final class Permit implements AutoCloseable {
        private final JudgeConcurrencyLimiter owner;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private Permit(JudgeConcurrencyLimiter owner) {
            this.owner = owner;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                owner.releasePermit();
            }
        }
    }
}
