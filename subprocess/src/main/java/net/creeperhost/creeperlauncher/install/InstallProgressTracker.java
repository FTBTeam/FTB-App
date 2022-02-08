package net.creeperhost.creeperlauncher.install;

import com.google.gson.Gson;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstallInstanceData;
import net.creeperhost.creeperlauncher.install.tasks.TaskProgressListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 8/2/22.
 */
public class InstallProgressTracker {

    private static final Gson GSON = new Gson();

    private final InstallInstanceData data;
    private final Map<Long, String> finishedFiles = new HashMap<>();
    private InstallStage currentStage = InstallStage.INIT;

    private double progress;
    private long speed;
    private long currentBytes;
    private long overallBytes;

    private long lastNonImportant = -1;

    private long lastSpeedTime;
    private long lastSpeedBytes;

    public InstallProgressTracker(InstallInstanceData data) {
        this.data = data;
        sendUpdate(true);
    }

    public void nextStage(InstallStage stage) {
        assert currentStage.ordinal() + 1 == stage.ordinal();
        currentStage = stage;
        currentBytes = 0;
        overallBytes = 0;
        sendUpdate(true);
    }

    public void submitFiles(List<DlFile> files) {
        // TODO this is disgusting.
        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "files", GSON.toJson(files), ""));
    }

    public void fileFinished(long id) {
        synchronized (finishedFiles) {
            finishedFiles.put(id, "downloaded");
        }
        sendUpdate(false);
    }

    public TaskProgressListener listenerForStage() {
        return new TaskProgressListener() {
            @Override
            public void start(long total) {
                overallBytes = total;
            }

            @Override
            public void update(long processed) {
                currentBytes = processed;
                progress = ((double) currentBytes / overallBytes) * 100D;
                sendUpdate(false);
            }

            @Override
            public void finish(long total) {
            }
        };
    }

    private void sendUpdate(boolean important) {
        long time = System.currentTimeMillis();
        if (!important) {
            // Rate limit non-important messages to every 100 millis
            if (lastNonImportant != -1 && time - 100 < lastNonImportant) {
                return;
            }
            lastNonImportant = time;
        } else {
            lastNonImportant = -1;
        }

        if (!finishedFiles.isEmpty()) {
            synchronized (finishedFiles) {
                Settings.webSocketAPI.sendMessage(new InstallInstanceData.FilesEvent(Map.copyOf(finishedFiles)));
                finishedFiles.clear();
            }
        }

        if (currentBytes > 0 && (time / 1000L) - (lastSpeedTime / 1000L) > 0) {

            speed = (currentBytes - lastSpeedBytes) / ((time / 1000L) - (lastSpeedTime / 1000L)) * 8;

            lastSpeedTime = time;
            lastSpeedBytes = currentBytes;
        }

        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Progress(
                data,
                progress,
                speed,
                currentBytes,
                overallBytes,
                currentStage
        ));
    }

    public enum InstallStage {
        INIT,
        PREPARE,
        MODLOADER,
        DOWNLOADS,
        FINISHED
    }

    @SuppressWarnings ("ClassCanBeRecord") // No because Gson.
    public static class DlFile {

        public final long id;
        public final String name;

        public DlFile(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
