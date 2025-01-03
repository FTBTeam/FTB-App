package dev.ftb.app.util;

import dev.ftb.app.install.tasks.TaskProgressListener;
import net.covers1624.quack.net.download.DownloadListener;

public record QuackProgressAdapter(TaskProgressListener progressListener) implements DownloadListener {

    //@formatter:off
    @Override public void connecting() { }
    @Override public void start(long expectedLen) { progressListener.start(expectedLen); }
    @Override public void update(long processedBytes) { progressListener.update(processedBytes); }
    @Override public void finish(long totalProcessed) { progressListener.finish(totalProcessed); }
    //@formatter:on
}
