package com.tms.lib.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class AsyncWorkerPool implements DisposableBean {
    private static final int MIN_POOL_SIZE = 2;
    private final ExecutorService asyncExecutor;
    private int poolSize;

    public AsyncWorkerPool(@Value("${asyncworker.poolsize:2}") int poolSize) {
        if (poolSize < MIN_POOL_SIZE) {
            throw new IllegalArgumentException(String.format("Async worker pool size cannot be less than %s", MIN_POOL_SIZE));
        } else {
            this.poolSize = poolSize;
            this.asyncExecutor = Executors.newFixedThreadPool(this.poolSize);
            log.info(String.format("Starting AsyncWorkerPool with pool size %d", this.poolSize));
        }
    }

    public void queueJob(Callable<?> job) {
        this.asyncExecutor.submit(job);
    }

    public void queueExec(Runnable r) {
        this.asyncExecutor.execute(r);
    }

    public void queueExec(final Runnable r, long timeout) {
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                AsyncWorkerPool.this.queueExec(r);
            }
        }, timeout);
    }

    public int getPoolSize() {
        return this.poolSize;
    }

    public void destroy() throws Exception {
        this.asyncExecutor.shutdownNow();
        log.info("Async worker shutdown");
    }
}
