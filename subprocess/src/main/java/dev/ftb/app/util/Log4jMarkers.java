package dev.ftb.app.util;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class Log4jMarkers {

    public static final Marker MINECRAFT = MarkerManager.getMarker("MINECRAFT");
    public static final Marker NO_SENTRY = MarkerManager.getMarker("NO_SENTRY");
    public static final Marker SENTRY_ONLY = MarkerManager.getMarker("SENTRY_ONLY");
}
