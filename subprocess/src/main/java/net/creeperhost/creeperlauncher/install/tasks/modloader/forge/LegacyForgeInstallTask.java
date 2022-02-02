package net.creeperhost.creeperlauncher.install.tasks.modloader.forge;

import net.creeperhost.creeperlauncher.install.tasks.TaskProgressListener;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import org.jetbrains.annotations.Nullable;

/**
 * Forge 'jarmod' versions.
 * <p>
 * Created by covers1624 on 2/2/22.
 */
public class LegacyForgeInstallTask extends AbstractForgeInstallTask {

    private final String mcVersion;
    private final String forgeVersion;

    public LegacyForgeInstallTask(String mcVersion, String forgeVersion) {
        this.mcVersion = mcVersion;
        this.forgeVersion = forgeVersion;
    }

    @Override
    public void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable {

    }
}
