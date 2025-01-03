package dev.ftb.app.api.handlers.other;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import net.covers1624.quack.util.HashUtils;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.other.FileHashData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.storage.settings.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

public class FileHashHandler implements IMessageHandler<FileHashData> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(FileHashData data) {
        Path file = Settings.getInstancesDir().resolve(data.uuid).resolve(data.filePath);
        WebSocketHandler.sendMessage(new FileHashData.Reply(data, safeHash(Hashing.md5(), file), safeHash(Hashing.sha256(), file)));
    }

    private static String safeHash(HashFunction func, Path file) {
        try {
            return HashUtils.hash(func, file).toString();
        } catch (IOException e) {
            LOGGER.error("Failed to hash file. {}", file, e);
            return "error - " + e.getMessage();
        }
    }
}
