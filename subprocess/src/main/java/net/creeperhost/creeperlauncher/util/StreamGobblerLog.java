package net.creeperhost.creeperlauncher.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class StreamGobblerLog {
    public static CompletableFuture<Void> redirectToLogger(final InputStream inputStream, final Consumer<String> logLineConsumer) {
        return CompletableFuture.runAsync(() -> {
            try (
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            ) {
                String line;
                while((line = bufferedReader.readLine()) != null) {
                    logLineConsumer.accept(line);
                }
            } catch (IOException ignored) {
            }
        });
    }
}
