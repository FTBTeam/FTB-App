package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.instances.CheckCurseZipData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.modpack.ModpackManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.creeperhost.creeperlauncher.api.handlers.instances.InstallInstanceHandler.prepareCurseImport;

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
