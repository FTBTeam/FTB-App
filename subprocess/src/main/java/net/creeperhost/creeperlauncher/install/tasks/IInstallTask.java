package net.creeperhost.creeperlauncher.install.tasks;

import java.util.concurrent.CompletableFuture;

public interface IInstallTask<T>
{
    CompletableFuture<T> execute();

    Double getProgress();
}
