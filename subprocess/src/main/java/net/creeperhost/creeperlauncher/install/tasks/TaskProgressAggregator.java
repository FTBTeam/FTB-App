package net.creeperhost.creeperlauncher.install.tasks;

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
