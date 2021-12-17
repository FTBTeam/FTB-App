package net.creeperhost.creeperlauncher.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.covers1624.quack.util.SneakyUtils.sneak;

public class StreamGobblerLog {

    public static CompletableFuture<Void> redirectToLogger(final InputStream inputStream, final Consumer<String> logLineConsumer) {
        return CompletableFuture.runAsync(sneak(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logLineConsumer.accept(line);
                }
            }
        }));
    }
}
