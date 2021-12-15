package net.creeperhost.creeperlauncher.util;

import net.covers1624.quack.net.download.DownloadListener;
import net.creeperhost.creeperlauncher.install.tasks.TaskProgressListener;

/**
 * Created by covers1624 on 15/12/21.
 */
public record QuackProgressAdapter(TaskProgressListener progressListener) implements DownloadListener {

    //@formatter:off
    @Override public void connecting() { }
    @Override public void start(long expectedLen) { progressListener.start(expectedLen); }
    @Override public void update(long processedBytes) { progressListener.update(processedBytes); }
    @Override public void finish(long totalProcessed) { progressListener.finish(totalProcessed); }
    //@formatter:on
}
