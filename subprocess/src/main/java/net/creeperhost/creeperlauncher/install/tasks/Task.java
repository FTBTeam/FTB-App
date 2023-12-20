package net.creeperhost.creeperlauncher.install.tasks;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.install.ProgressTracker;
import net.creeperhost.creeperlauncher.util.CancellationToken;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A Task.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
public interface Task {

    ExecutorService TASK_POOL = new ThreadPoolExecutor(
            Settings.getThreadLimit(),
            Settings.getThreadLimit(),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );

    /**
     * Execute this task.
     *
     * @param listener The listener to receive updates about this task.
     * @throws Throwable If any exception is thrown whilst executing the task.
     */
    // TODO, phase out Nullable from both these parameters. Promote usage of NOP.
    void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable;

    /**
     * Wrap the current task into a new {@link Task} which calls
     * {@link ProgressTracker#stepFinished()} when the task finishes.
     *
     * @param tracker The tracker.
     * @return The {@link Task}.
     */
    default Task wrapStepComplete(ProgressTracker tracker) {
        if (tracker == ProgressTracker.NOP) return this;

        return (c, l) -> {
            execute(c, l);
            tracker.stepFinished();
        };
    }
}
