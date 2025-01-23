package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.CheckCurseZipData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.data.modpack.ModpackManifest;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static dev.ftb.app.api.handlers.instances.InstallInstanceHandler.prepareCurseImport;

public class CheckCurseZip implements IMessageHandler<CheckCurseZipData> {
    
    @Override
    public void handle(CheckCurseZipData data) {
        var path = Path.of(data.path);
        if (Files.notExists(path)) {
            WebSocketHandler.sendMessage(new CheckCurseZipData.Reply(data, false, "Modpack import file does not exist."));
            return;
        }
        if (!Files.isRegularFile(path)) {
            WebSocketHandler.sendMessage(new CheckCurseZipData.Reply(data, false, "Modpack import file is not a file."));
            return;
        }
        try {
            Pair<ModpackManifest, ModpackVersionManifest> manifests = prepareCurseImport(path);
            if (manifests == null) {
                WebSocketHandler.sendMessage(new CheckCurseZipData.Reply(data, false, "File is not a valid CurseForge modpack."));
                return;
            }
        } catch (IOException e) {
            WebSocketHandler.sendMessage(new CheckCurseZipData.Reply(data, false, "File is not a valid CurseForge modpack."));
            return;
        }
        
        WebSocketHandler.sendMessage(new CheckCurseZipData.Reply(data, true, ""));
    }
}
