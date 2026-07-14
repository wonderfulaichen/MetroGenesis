package com.metrogenesis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Global asynchronous executor backed by a single {@link ForkJoinPool}.
 * Provides helper methods for submitting tasks without creating
 * additional thread pools across the project.
 */
public final class AsyncExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger("MetroGenesis/AsyncExecutor");

    private AsyncExecutor() {
    }

    private static final class Holder {
        private static final ForkJoinPool POOL = createPool();

        private static ForkJoinPool createPool() {
            int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
            LOGGER.info("AsyncExecutor pool initialized with parallelism={} (available={})",
                    parallelism, Runtime.getRuntime().availableProcessors());
            return new ForkJoinPool(parallelism);
        }
    }

    /**
     * Submits a value-producing task to the shared pool.
     */
    public static <T> CompletableFuture<T> submit(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, Holder.POOL);
    }

    /**
     * Executes a fire-and-forget task on the shared pool.
     */
    public static void execute(Runnable task) {
        Holder.POOL.execute(task);
    }
}
