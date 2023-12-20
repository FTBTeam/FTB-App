package net.creeperhost.creeperlauncher.install.tasks;

/**
 * Provided to individual tasks to provide progress feedback.
 * <p>
 * Created by covers1624 on 15/12/21.
 */
public interface TaskProgressListener {

    TaskProgressListener NOP = new Nop();

    /**
     * Called to indicate the task has started its operation.
     * <p>
     * This may be called at any point again to indicate a restart.
     *
     * @param total The total amount of unit this tasks does.
     */
    void start(long total);

    /**
     * Called to indicate a progress update of the task.
     *
     * @param processed The amount of unit the task has processed in total.
     */
    void update(long processed);

    /**
     * Called to indicate the task has finished.
     *
     * @param total The total amount of unit the task did.
     */
    void finish(long total);

    // @formatter:off
    final class Nop implements TaskProgressListener {
        private Nop() { }
        @Override public void start(long total) { }
        @Override public void update(long processed) { }
        @Override public void finish(long total) { }
    }
    // @formatter:on
}
