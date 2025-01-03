package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.install.OperationProgressTracker;

import java.util.Map;

/**
 * Created by covers1624 on 18/8/23.
 */
public class OperationProgressUpdateData extends BaseData {

    /**
     * What kind of operation this is. Sync/Install, etc.
     */
    public final String operation;
    /**
     * Any additional metadata about the operation. Instance UUID, etc.
     */
    public final Map<String, String> metadata;

    /**
     * The current stage. Mostly specific to the operation type.
     * <p>
     * Will include default stages.
     *
     * @see OperationProgressTracker.Stage
     * @see OperationProgressTracker.DefaultStages
     */
    public final OperationProgressTracker.Stage stage;

    public final int steps;
    public final int totalSteps;

    /**
     * Overall percentage of current stage as calculated by the amount of bytes transferred.
     */
    public final double percent;

    /**
     * The speed in bits per second.
     */
    public final long speed;
    /**
     * The number of bytes transferred
     */
    public final long bytes;
    /**
     * The number of total bytes expected to be transferred.
     */
    public final long totalBytes;

    public OperationProgressUpdateData(String operation, Map<String, String> metadata, OperationProgressTracker.Stage stage, int steps, int totalSteps, double percent, long speed, long bytes, long totalBytes) {
        type = "operationUpdate";
        this.operation = operation;
        this.metadata = metadata;
        this.stage = stage;
        this.steps = steps;
        this.totalSteps = totalSteps;
        this.percent = percent;
        this.speed = speed;
        this.bytes = bytes;
        this.totalBytes = totalBytes;
    }
}
