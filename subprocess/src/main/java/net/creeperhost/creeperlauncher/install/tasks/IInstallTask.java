package net.creeperhost.creeperlauncher.install.tasks;

import java.util.concurrent.CompletableFuture;

// TODO, We should move away from directly returning a CompletableFuture here.
//  The installer should build the list of tasks and execute them sequentially for greater control.
//  As it sits now, there is no way of controlling the exact task execution order, _unless_ the tasks
//  are submitted to a specific executor, in that case they are executed in the order they are constructed,
//  but nothing can control _when_ they execute.
@Deprecated
public interface IInstallTask<T> {

    CompletableFuture<T> execute();

    Double getProgress();
}
