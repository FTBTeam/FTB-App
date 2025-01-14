package dev.ftb.app.install.tasks;

/**
 * Provided to individual tasks to provide progress feedback.
 */
public interface TaskProgressListener {

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
}
