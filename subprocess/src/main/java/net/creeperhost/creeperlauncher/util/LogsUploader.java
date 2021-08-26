package net.creeperhost.creeperlauncher.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.os.OS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by covers1624 on 24/2/21.
 */
public class LogsUploader {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Uploads the UI and Backend Debug logs to pste.ch
     *
     * @param uiVersion    The UI Version.
     * @param frontendLogs The Frontend logs.
     * @return The pste.ch code, or null if an error occurred.
     */
    @Nullable
    public static String uploadUILogs(@Nullable String uiVersion, @Nullable String frontendLogs) {
        String debugLogs = getDebugLog();
        String uploadData = "UI Version: " + (uiVersion != null ? uiVersion : "Unknown") + "\n" +
                "App Version: " + Constants.APPVERSION + "\n" +
                "Platform: " + Constants.PLATFORM + "\n" +
                "Operating System: " + OS.CURRENT + "\n" +
                "\n" +
                "\n" +
                padString(" debug.log ") + "\n" +
                (debugLogs == null ? "Not available" : debugLogs) + "\n" +
                "\n" +
                "\n" +
                padString(" main.log ") + "\n" +
                (frontendLogs == null ? "Not available" : frontendLogs);

        return uploadPaste(uploadData);
    }

    /**
     * Reads the latest backend debug.log file from disk.
     *
     * @return The string content, or null if an error occurred.
     */
    @Nullable
    public static String getDebugLog() {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext();
            for (org.apache.logging.log4j.core.Logger logger : context.getLoggers()) {
                for (Appender appender : logger.getAppenders().values()) {
                    if (appender instanceof AbstractOutputStreamAppender) {
                        ((AbstractOutputStreamAppender<?>) appender).getManager().flush();
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        Path debugLogFile = Constants.getDataDir().resolve("logs/debug.log");

        if (Files.exists(debugLogFile)) {
            try {
                return Files.readString(debugLogFile);
            } catch (IOException e) {
                LOGGER.warn("Failed to load debug logs.", e);
            }
        }
        return null;
    }

    /**
     * Uploads the given data to pste.ch
     *
     * @param data The data to upload.
     * @return The pste.ch code, or null if an error occured.
     */
    @Nullable
    public static String uploadPaste(@Nullable String data) {
        String result = WebUtils.postWebResponse("https://pste.ch/documents", data == null ? "No data available." : data, "text/plain; charset=UTF-8");
        try {
            JsonObject objResponse = GsonUtils.GSON.fromJson(result, JsonObject.class);
            JsonElement key = objResponse.get("key");
            if (key == null || !key.isJsonPrimitive()) return null;

            return key.getAsString();
        } catch (Throwable e) {
            return null;
        }
    }

    private static String padString(String stringToPad) {
        int desiredLength = 86;
        char padChar = '=';
        int strLen = stringToPad.length();
        float halfLength = ((float) desiredLength - (float) strLen) / (float) 2;
        int leftPad;
        int rightPad;
        if (((int) halfLength) != halfLength) {
            leftPad = (int) halfLength + 1;
            rightPad = (int) halfLength;
        } else {
            leftPad = rightPad = (int) halfLength;
        }

        String padCharStr = String.valueOf(padChar);

        return padCharStr.repeat(leftPad).concat(stringToPad).concat(padCharStr.repeat(rightPad));
    }
}
