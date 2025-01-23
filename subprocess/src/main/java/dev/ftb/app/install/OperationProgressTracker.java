package dev.ftb.app.install;

import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.OperationProgressUpdateData;
import dev.ftb.app.install.tasks.TaskProgressListener;

import java.util.Map;

public class OperationProgressTracker {

    /**
     * How often non-important updates will be sent to the UI.
     */
    private static final long NON_IMPORTANT_THROTTLE_MS = 250;

    private final String type;
    private final Map<String, String> meta;
    private Stage stage = DefaultStages.NOT_STARTED;

    private int steps;
    private int completedSteps;

    private long speed;
    private long currentBytes;
    private long overallBytes;

    private long lastNonImportant = -1;

    private long lastSpeedTime;
    private long lastSpeedBytes;

    public OperationProgressTracker(String type, Map<String, String> meta) {
        this.type = type;
        this.meta = meta;

        sendUpdate(true);
    }

    public void nextStage(Stage stage) {
        nextStage(stage, -1);
    }

    public void nextStage(Stage stage, int steps) {
        if (this.stage == DefaultStages.FINISHED) {
            throw new IllegalArgumentException("Already finished, can't do more do.");
        }
        this.stage = stage;
        this.steps = steps;

        // Clear state
        completedSteps = -1;
        currentBytes = 0;
        overallBytes = 0;
        lastSpeedTime = 0;
        lastSpeedBytes = 0;
        sendUpdate(true);
    }

    public synchronized void stepFinished() {
        completedSteps++;
    }

    public TaskProgressListener listenerForStage() {
        // @formatter:off
        return new TaskProgressListener() {
            @Override public void start(long total) { overallBytes = total; }
            @Override public void update(long processed) { currentBytes = processed; sendUpdate(false); }
            @Override public void finish(long total) { }
        };
        // @formatter:on
    }

    public void finished() {
        nextStage(DefaultStages.FINISHED);
        sendUpdate(true);
    }

    private void sendUpdate(boolean important) {
        long time = System.currentTimeMillis();
        if (!important) {
            // Rate limit non-important messages to every 100 millis
            if (lastNonImportant != -1 && time - NON_IMPORTANT_THROTTLE_MS < lastNonImportant) {
                return;
            }
            lastNonImportant = time;
        } else {
            lastNonImportant = -1;
        }

        long speedTime = time / 1000L;
        long dTime = speedTime - lastSpeedTime;
        if (currentBytes > 0 &&  dTime > 0) {
            speed = ((currentBytes - lastSpeedBytes) / dTime) * 8;

            lastSpeedTime = speedTime;
            lastSpeedBytes = currentBytes;
        }

        WebSocketHandler.sendMessage(new OperationProgressUpdateData(
                type,
                meta,
                stage,
                completedSteps,
                steps,
                computeProgress(currentBytes, overallBytes),
                speed,
                currentBytes,
                overallBytes
        ));
    }

    private double computeProgress(long currentBytes, long overallBytes) {
        if (overallBytes == 0) {
            return 0;
        }

        return (double) currentBytes / (double) overallBytes * 100.0D;
    }

    /**
     * Represents a stage.
     * <p>
     * This should only ever be implemented on enum constants.
     * <p>
     * It will be serialized by Gson.
     */
    public interface Stage {
    }

    public enum DefaultStages implements Stage {
        NOT_STARTED,
        FINISHED,
    }
}
