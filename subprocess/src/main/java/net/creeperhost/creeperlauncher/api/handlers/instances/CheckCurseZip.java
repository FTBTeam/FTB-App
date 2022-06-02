package net.creeperhost.creeperlauncher.api.handlers.instances;

import com.google.gson.JsonSyntaxException;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.CheckCurseZipData;
import net.creeperhost.creeperlauncher.api.data.instances.CheckShareCodeData;
import net.creeperhost.creeperlauncher.api.data.instances.InstallInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.modpack.ModpackManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.data.modpack.ShareManifest;
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
            Settings.webSocketAPI.sendMessage(new CheckCurseZipData.Reply(data, false, "Modpack import file does not exist."));
            return;
        }
        if (!Files.isRegularFile(path)) {
            Settings.webSocketAPI.sendMessage(new CheckCurseZipData.Reply(data, false, "Modpack import file is not a file."));
            return;
        }
        try {
            Pair<ModpackManifest, ModpackVersionManifest> manifests = prepareCurseImport(path);
            if (manifests == null) {
                Settings.webSocketAPI.sendMessage(new CheckCurseZipData.Reply(data, false, "Failed to import curse pack."));
                return;
            }
        } catch (IOException e) {
            Settings.webSocketAPI.sendMessage(new CheckCurseZipData.Reply(data, false, "Failed to import curse pack."));
        }
        
        Settings.webSocketAPI.sendMessage(new CheckCurseZipData.Reply(data, true, ""));
    }
}
