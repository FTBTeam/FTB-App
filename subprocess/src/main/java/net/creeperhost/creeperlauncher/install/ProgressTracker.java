package net.creeperhost.creeperlauncher.install;

import net.creeperhost.creeperlauncher.install.tasks.TaskProgressListener;

/**
 * Created by covers1624 on 19/12/23.
 */
public interface ProgressTracker {

    ProgressTracker NOP = new ProgressTracker.Nop();

    default void nextStage(Stage stage) {
        nextStage(stage, -1);
    }

    void nextStage(Stage stage, int steps);

    void setDynamicStepCount(int steps);

    void setCustomStatus(String status);

    void stepFinished();

    TaskProgressListener dynamicListener();

    TaskProgressListener listenerForStage();

    void finished();

    enum DefaultStages implements Stage {
        NOT_STARTED,
        FINISHED,
    }

    /**
     * Represents a stage.
     * <p>
     * This should only ever be implemented on enum constants.
     * <p>
     * It will be serialized by Gson.
     */
    interface Stage {
    }

    // @formatter:off
    class Nop implements ProgressTracker {
        private Nop() { }
        @Override public void nextStage(Stage stage, int steps) { }
        @Override public void setDynamicStepCount(int steps) { }
        @Override public void setCustomStatus(String status) { }
        @Override public void stepFinished() { }
        @Override public TaskProgressListener dynamicListener() { return TaskProgressListener.NOP; }
        @Override public TaskProgressListener listenerForStage() { return TaskProgressListener.NOP; }
        @Override public void finished() { }
    }
    // @formatter:on
}
