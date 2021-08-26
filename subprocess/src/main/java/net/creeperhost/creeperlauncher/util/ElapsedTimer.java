package net.creeperhost.creeperlauncher.util;

import java.util.concurrent.TimeUnit;

/**
 * Created by covers1624 on 13/1/21.
 */
public class ElapsedTimer {

    private final long start;

    public ElapsedTimer() {
        start = System.nanoTime();
    }

    public long elapsedNanos() {
        return System.nanoTime() - start;
    }

    public String elapsedStr() {
        return timeString(elapsedNanos());
    }

    //TODO, this needs to support Seconds.
    public static String timeString(long delta) {
        long millis = TimeUnit.NANOSECONDS.toMillis(delta);

        String s;
        if (millis >= 5) {
            s = millis + "ms(" + delta + "ns)";
        } else {
            s = delta + "ns";
        }
        return s;
    }
}
