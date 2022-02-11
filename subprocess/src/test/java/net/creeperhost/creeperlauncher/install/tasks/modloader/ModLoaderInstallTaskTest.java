package net.creeperhost.creeperlauncher.install.tasks.modloader;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by covers1624 on 2/2/22.
 */
public class ModLoaderInstallTaskTest {

    @Test
    public void testForgeRanges() {
        assertTrue(ModLoaderInstallTask.FORGE_CLIENT_ZIP.containsVersion(new DefaultArtifactVersion("1.3.2.1")));
        assertTrue(ModLoaderInstallTask.FORGE_CLIENT_ZIP.containsVersion(new DefaultArtifactVersion("4.0.0.182")));
        assertFalse(ModLoaderInstallTask.FORGE_CLIENT_ZIP.containsVersion(new DefaultArtifactVersion("4.0.0.183")));

        assertFalse(ModLoaderInstallTask.FORGE_UNIVERSAL_ZIP.containsVersion(new DefaultArtifactVersion("4.0.0.182")));
        assertTrue(ModLoaderInstallTask.FORGE_UNIVERSAL_ZIP.containsVersion(new DefaultArtifactVersion("4.0.0.183")));
        assertTrue(ModLoaderInstallTask.FORGE_UNIVERSAL_ZIP.containsVersion(new DefaultArtifactVersion("8.9.0.751")));
        assertFalse(ModLoaderInstallTask.FORGE_UNIVERSAL_ZIP.containsVersion(new DefaultArtifactVersion("8.9.0.752")));

        assertFalse(ModLoaderInstallTask.FORGE_INSTALLER_JAR.containsVersion(new DefaultArtifactVersion("7.8.0.683")));
        assertTrue(ModLoaderInstallTask.FORGE_INSTALLER_JAR.containsVersion(new DefaultArtifactVersion("7.8.0.684")));
        assertTrue(ModLoaderInstallTask.FORGE_INSTALLER_JAR.containsVersion(new DefaultArtifactVersion("39.0.64")));

        assertFalse(ModLoaderInstallTask.FORGE_LEGACY_INSTALL.containsVersion(new DefaultArtifactVersion("1.1")));
        assertTrue(ModLoaderInstallTask.FORGE_LEGACY_INSTALL.containsVersion(new DefaultArtifactVersion("1.2")));
        assertTrue(ModLoaderInstallTask.FORGE_LEGACY_INSTALL.containsVersion(new DefaultArtifactVersion("1.2.5")));
        assertTrue(ModLoaderInstallTask.FORGE_LEGACY_INSTALL.containsVersion(new DefaultArtifactVersion("1.5")));
        assertTrue(ModLoaderInstallTask.FORGE_LEGACY_INSTALL.containsVersion(new DefaultArtifactVersion("1.5.2")));
        assertFalse(ModLoaderInstallTask.FORGE_LEGACY_INSTALL.containsVersion(new DefaultArtifactVersion("1.6")));
    }
}
