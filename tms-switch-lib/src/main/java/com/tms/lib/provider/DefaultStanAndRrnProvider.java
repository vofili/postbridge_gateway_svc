package com.tms.lib.provider;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class DefaultStanAndRrnProvider implements StanProvider, RrnProvider {

    private static final AtomicLong STAN_COUNTER = new AtomicLong();
    private static final AtomicLong RRN_COUNTER = new AtomicLong();

    @Override
    public synchronized String getNextStan() {
        if (STAN_COUNTER.longValue() == 999999L) {
            STAN_COUNTER.set(0);
        }
        return String.format("%06d", STAN_COUNTER.incrementAndGet());
    }

    @Override
    public synchronized String getNextRrn() {
        if (RRN_COUNTER.longValue() == 999999999999L) {
            RRN_COUNTER.set(0);
        }
        return String.format("%012d", RRN_COUNTER.incrementAndGet());
    }
}
