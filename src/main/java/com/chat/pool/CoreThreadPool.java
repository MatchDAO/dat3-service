package com.chat.pool;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Log4j2
public class CoreThreadPool {
    private static final int POOL_SIZE = 10;
    private static ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(POOL_SIZE);

    public static synchronized void execute(String taskName, Runnable runnable) {
        int activateThreads = pool.getActiveCount();
        if (activateThreads >= POOL_SIZE) {
            log.warn("Pulling a task " + taskName + " with 0 thread usable.");
        } else {
            log.info("Pulling a task " + taskName + " with " + activateThreads +" thread(s) running.");
        }
        pool.execute(runnable);
    }

    public static synchronized Future<?> submit(String taskName, Runnable runnable) {
        int activateThreads = pool.getActiveCount();
        if (activateThreads >= POOL_SIZE) {
            log.warn("Pulling a task " + taskName + " with 0 thread usable.");
        } else {
            log.info("Pulling a task " + taskName + " with " + activateThreads +" thread(s) running.");
        }
        return pool.submit(runnable);
    }
}
