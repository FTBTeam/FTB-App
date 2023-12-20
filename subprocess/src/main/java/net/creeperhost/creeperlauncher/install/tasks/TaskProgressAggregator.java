package net.creeperhost.creeperlauncher.install.tasks;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created by covers1624 on 15/12/21.
 */
public class TaskProgressAggregator implements TaskProgressListener {

    protected final TaskProgressListener parent;

    private long lastProcessed = 0;

    private long processed = 0;

    public TaskProgressAggregator(TaskProgressListener parent) {
        this.parent = parent;
    }

    public static void aggregate(TaskProgressListener root, long estimatedSize, Consumer<TaskProgressListener> cons) {
        if (root == NOP) {
            cons.accept(NOP);
            return;
        }

        root.start(estimatedSize);
        TaskProgressAggregator aggregator = new TaskProgressAggregator(root);
        cons.accept(aggregator);
        root.finish(aggregator.getProcessed());
    }

    @Override
    public void update(long processed) {
        this.processed += (processed - lastProcessed);
        parent.update(this.processed);
        lastProcessed = processed;
    }

    @Override
    public void start(long total) {
        lastProcessed = 0;
    }

    @Override
    public void finish(long total) { }

    public long getProcessed() {
        return processed;
    }
}
