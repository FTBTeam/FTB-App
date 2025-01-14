package dev.ftb.app.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import static net.covers1624.quack.util.SneakyUtils.sneak;

public class StreamGobblerLog {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    private Thread thread;

    private String name = "Stream Gobbler";
    @Nullable
    private InputStream input;
    @Nullable
    private Consumer<String> output;

    private boolean stop = false;

    public StreamGobblerLog setName(String name) {
        this.name = name;
        return this;
    }

    public StreamGobblerLog setInput(InputStream is) {
        if (input != null) throw new IllegalArgumentException("Unable to change input.");
        input = is;
        return this;
    }

    public StreamGobblerLog setOutput(Consumer<String> output) {
        if (this.output != null) throw new IllegalArgumentException("Unable to change input.");
        this.output = output;
        return this;
    }

    public StreamGobblerLog start() {
        if (input == null) throw new IllegalStateException("Input not set.");
        if (output == null) throw new IllegalStateException("Output not set.");
        if (thread != null) throw new IllegalStateException("Cannot be started more than once.");
        thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                String line;
                while (!stop && (line = reader.readLine()) != null) {
                    output.accept(line);
                }
            } catch (IOException ex) {
                // Maybe a bit verbose, sometimes these may occur on normal shutdown.
                LOGGER.warn("IOThread exiting with IOException.", ex);
            }
        });
        thread.setName(name);
        thread.setDaemon(true);
        thread.start();

        return this;
    }

    public void stop() {
        stop = true;
    }
}
