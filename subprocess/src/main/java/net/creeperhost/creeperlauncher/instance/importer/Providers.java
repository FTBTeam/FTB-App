package net.creeperhost.creeperlauncher.instance.importer;

import com.google.gson.JsonElement;
import net.covers1624.quack.platform.OperatingSystem;
import net.creeperhost.creeperlauncher.instance.importer.meta.InstanceSummary;
import net.creeperhost.creeperlauncher.instance.importer.providers.*;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.covers1624.quack.platform.OperatingSystem.*;
import static net.creeperhost.creeperlauncher.instance.importer.Providers.Internal.*;

/**
 * Created by covers1624 on 17/1/24.
 */
public enum Providers {
    PRISM(
            new PrismProvider(),
            Map.of(
                    MACOS, () -> userHome().resolve("/Library/Application Support/PrismLauncher/instances/"),
                    LINUX, () -> userHome().resolve(".local/share/PrismLauncher/instances/")
            )
    ),
    MULTIMC(
            new MultiMcProvider(),
            Map.of(
                    MACOS, () -> Path.of("/Applications/MultiMC.app/Data/instances/"),
                    LINUX, () -> userHome().resolve(".local/share/multimc/instances/")
            )
    ),
    ATLAUNCHER(
            new AtLauncherProvider(),
            Map.of(
                    MACOS, () -> Path.of("/Applications/ATLauncher.app/Contents/Java/instances")
            )
    ),
    GDLAUNCHER(
            new GdLauncherProvider(),
            Map.of(
                    MACOS, () -> userHome().resolve("/Library/Application Support/gdlauncher_next/instances") // TODO: Is next their main launcher?
            )
    ),
    CURSEFORGE(
            new CurseForgeProvider(),
            Map.of(
                    WINDOWS, () -> appData().resolve("CurseForge"),
                    MACOS, () -> userHome().resolve("Library/Application Support/CurseForge"),
                    LINUX, () -> userHome().resolve(".config/CurseForge")
            )
    ) {
        @Override
        public Path getInstancesDir() {
            Path basePath = super.getInstancesDir();
            JsonElement settings = GsonUtils.parseRawSafe(basePath.resolve("settings.json"));
            if (settings == null) return basePath;

            String moddedDir = GsonUtils.getNestedField("minecraft.moddingFolder", settings, JsonElement::getAsString);
            if (StringUtils.isEmpty(moddedDir)) {
                return basePath;
            }

            Path instancesDir = Path.of(moddedDir).resolve("Instances");
            if (Files.notExists(instancesDir)) {
                return basePath;
            }

            return instancesDir;
        }
    },
    MODRINTH(
            new ModrinthProvider(),
            Map.of(
                    MACOS, () -> userHome().resolve("/Library/Application Support/com.modrinth.theseus/profiles/")
            )
    );

    private static final Logger LOGGER = LogManager.getLogger();

    public final InstanceProvider provider;
    private final Map<OperatingSystem, Supplier<Path>> instancesDirs;

    Providers(InstanceProvider provider, Map<OperatingSystem, Supplier<Path>> instancesDirs) {
        this.provider = provider;
        this.instancesDirs = instancesDirs;
    }

    public Path getInstancesDir() {
        OperatingSystem os = OperatingSystem.current();
        Supplier<Path> supp = instancesDirs.get(os);
        if (supp == null) {
            throw new NotImplementedException("We don't current know where instances for " + this + " are installed on " + os);
        }
        return supp.get();
    }

    public List<InstanceSummary> findInstances(@Nullable Path overridePath) throws IOException {
        Path instancesDir = overridePath != null ? overridePath : getInstancesDir();
        if (Files.notExists(instancesDir)) return List.of();

        LOGGER.info("Finding instances for {} in {}..", this, instancesDir);
        try (Stream<Path> files = Files.list(instancesDir)) {
            return files
                    .filter(Files::isDirectory)
                    .map(provider::scanForInstance)
                    .filter(Objects::nonNull)
                    .peek(e -> LOGGER.info(" Found {}", e))
                    .collect(Collectors.toList());
        }
    }

    // Yay forward references! must be inside inner class as we can't declare these before the enum declarations.
    static class Internal {

        public static Path appData() {
            return Path.of(System.getenv("APPDATA"));
        }

        public static Path userHome() {
            return Path.of(System.getProperty("user.home"));
        }
    }
}
