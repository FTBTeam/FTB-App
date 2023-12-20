package net.creeperhost.creeperlauncher.install;

import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.instances.OperationProgressUpdateData;
import net.creeperhost.creeperlauncher.install.tasks.TaskProgressListener;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Created by covers1624 on 18/8/23.
 */
public class OperationProgressTracker implements ProgressTracker {

    /**
     * How often non-important updates will be sent to the UI.
     */
    private static final long NON_IMPORTANT_THROTTLE_MS = 250;

    private final String type;
    private final Map<String, String> meta;
    private Stage stage = ProgressTracker.DefaultStages.NOT_STARTED;

    private int steps;
    private int completedSteps;

    private long speed;
    private long currentBytes;
    private long overallBytes;

    private long lastNonImportant = -1;

    private long lastSpeedTime;
    private long lastSpeedBytes;

    private @Nullable String customStatus;

    public OperationProgressTracker(String type, Map<String, String> meta) {
        this.type = type;
        this.meta = meta;

        sendUpdate(true);
    }

    @Override
    public void nextStage(Stage stage, int steps) {
        if (this.stage == ProgressTracker.DefaultStages.FINISHED) {
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
        customStatus = null;
        sendUpdate(true);
    }

    @Override
    public void setDynamicStepCount(int steps) {
        this.steps = steps;
        completedSteps = -1;
        sendUpdate(true);
    }

    @Override
    public void setCustomStatus(String status) {
        customStatus = status;
        steps = -1;
        completedSteps = -1;
        currentBytes = 0;
        overallBytes = 0;
        lastSpeedTime = 0;
        lastSpeedBytes = 0;
        sendUpdate(true);
    }

    @Override
    public synchronized void stepFinished() {
        completedSteps++;
        sendUpdate(false);
    }

    @Override
    public TaskProgressListener dynamicListener() {
        currentBytes = 0;
        overallBytes = 0;
        lastSpeedTime = 0;
        lastSpeedBytes = 0;
        return listenerForStage();
    }

    @Override
    public TaskProgressListener listenerForStage() {
        // @formatter:off
        return new TaskProgressListener() {
            @Override public void start(long total) { overallBytes = total; }
            @Override public void update(long processed) { currentBytes = processed; sendUpdate(false); }
            @Override public void finish(long total) { }
        };
        // @formatter:on
    }

    @Override
    public void finished() {
        nextStage(ProgressTracker.DefaultStages.FINISHED);
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
        if (currentBytes > 0 && (speedTime) - (lastSpeedTime) > 0) {

            speed = ((currentBytes - lastSpeedBytes) / (speedTime - lastSpeedTime)) * 8;

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
                overallBytes,
                customStatus
        ));
    }

    private double computeProgress(long currentBytes, long overallBytes) {
        if (overallBytes == 0) {
            return 0;
        }

        return (double) currentBytes / (double) overallBytes * 100.0D;
    }
}
