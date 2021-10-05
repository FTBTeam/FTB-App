package net.creeperhost.creeperlauncher.install.tasks.http;

@FunctionalInterface
public interface IProgressUpdater
{
    void update(long currentBytes, long delta, long totalBytes, boolean done);
}
