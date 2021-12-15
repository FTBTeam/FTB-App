package net.creeperhost.creeperlauncher.install.tasks;

import org.jetbrains.annotations.Nullable;

/**
 * A Task.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
public interface Task<T> {

    /**
     * Execute this task.
     *
     * @param listener The listener to receive updates about this task.
     * @throws Throwable If any exception is thrown whilst executing the task.
     */
    void execute(@Nullable TaskProgressListener listener) throws Throwable;

    /**
     * Checks if executing this task would do nothing.
     *
     * @return If this task is redundant.
     */
    default boolean isRedundant() { return false; }

    /**
     * Returns the Optional result of this task.
     *
     * @return The task's result or <code>null</code> if the task has no result.
     */
    @Nullable
    T getResult();
}
