package dev.ftb.app.services;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.ftb.app.task.LongRunningTaskManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class AppThreads {
    private final ThreadPoolExecutor taskPool;
    
    private final ExecutorService messageHandlerPool;
    private final ExecutorService longRunningTaskPool;
    private final ExecutorService instanceLauncherPool;
    private final ScheduledExecutorService pingPongPool;
    private final ExecutorService modVersionCachePool;
    
    private final LongRunningTaskManager longRunningTaskManager;

    private final List<ExecutorService> allPools = new ArrayList<>();

    /// @param threadCount The amount of threads the Task Pool has.
    public AppThreads(int threadCount) {
        // TODO: this needs to be able to update dynamically based on the settings.
        this.taskPool = registerPool(new ThreadPoolExecutor(
            threadCount,
            threadCount,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            daemon("Task Executor %d")
        ));
        
        // Core size equals max size, so without this the keep-alive never applies and idle threads live forever
        this.taskPool.allowCoreThreadTimeOut(true);

        this.longRunningTaskPool = registerPool(Executors.newCachedThreadPool(daemon("Long Running Executor %d")));
        this.instanceLauncherPool = registerPool(Executors.newCachedThreadPool(daemon("Instance Launcher %d")));
        this.pingPongPool = registerPool(Executors.newSingleThreadScheduledExecutor(daemon("Ping Pong Executor %d")));
        this.modVersionCachePool = registerPool(Executors.newCachedThreadPool(daemon("ModVersionCache %d")));
        this.messageHandlerPool = registerPool(Executors.newCachedThreadPool(daemon("Message Handler %d")));
        
        this.longRunningTaskManager = new LongRunningTaskManager(this.longRunningTaskPool);
    }
    
    private <T extends ExecutorService> T registerPool(T pool) {
        allPools.add(pool);
        return pool;
    }

    public void setTaskThreadLimit(int limit) {
        // We need to call the specific methods in a certain order to avoid IllegalArgumentException.
        if (limit > taskPool.getMaximumPoolSize()) {
            taskPool.setMaximumPoolSize(limit);
            taskPool.setCorePoolSize(limit);
        } else {
            taskPool.setCorePoolSize(limit);
            taskPool.setMaximumPoolSize(limit);
        }
    }

    public void close() {
        for (ExecutorService pool : allPools) {
            pool.shutdown();
        }
        
        // Wait for all pools to terminate
        for (ExecutorService pool : allPools) {
            try {
                if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static ThreadFactory daemon(String nameFormat) {
        return new ThreadFactoryBuilder().setNameFormat(nameFormat).setDaemon(true).build();
    }
    
    public LongRunningTaskManager longRunningTaskManager() {
        return longRunningTaskManager;
    }

    public ExecutorService taskPool() {
        return taskPool;
    }

    public ExecutorService longRunningTaskPool() {
        return longRunningTaskPool;
    }

    public ExecutorService instanceLauncherPool() {
        return instanceLauncherPool;
    }

    public ScheduledExecutorService pingPongPool() {
        return pingPongPool;
    }

    public ExecutorService modVersionCachePool() {
        return modVersionCachePool;
    }

    public ExecutorService messageHandlerPool() {
        return messageHandlerPool;
    }
}
