package net.creeperhost.creeperlauncher.util;

import java.util.concurrent.CompletableFuture;

public class instanceEvent {
    private boolean async = true;
    private Runnable code;
    private String name = "anonymous";
    public instanceEvent(Runnable lambda)
    {
        code = lambda;
    }
    public instanceEvent(Runnable lambda, String name)
    {
        this.name = name;
        code = lambda;
    }
    public instanceEvent(Runnable lambda, boolean blocking)
    {
        async = !blocking;
        code = lambda;
    }
    public instanceEvent(Runnable lambda, String name, boolean blocking)
    {
        this.name = name;
        async = !blocking;
        code = lambda;
    }
    public CompletableFuture Run()
    {
        CompletableFuture ft = CompletableFuture.runAsync(code);
        if(!async) ft.join();
        return ft;
    }
}
