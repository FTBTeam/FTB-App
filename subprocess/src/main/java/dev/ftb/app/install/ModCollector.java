package dev.ftb.app.install;

import dev.ftb.app.data.mod.ModManifest;

/**
 * Created by covers1624 on 12/9/23.
 */
public interface ModCollector {

    /**
     * @return The MC Version to filter dependencies by.
     */
    String mcVersion();

    /**
     * @return The ModLoader to filter dependencies by.
     */
    String modLoader();

    /**
     * The mod does not exist.
     *
     * @param requestedBy The mod which requires this dep.
     * @param dep         The dependency.
     */
    void unavailableDependency(ModManifest requestedBy, ModManifest.Dependency dep);

    /**
     * The mod exists, but a compatible version does not exist.
     *
     * @param requestedBy The mod which requires this dep.
     * @param dep         The dependency.
     */
    void unsatisfiableDependency(ModManifest requestedBy, ModManifest dep);

    /**
     * Add mod to the list to be downloaded.
     *
     * @param mod     The mod.
     * @param version The version.
     */
    void addMod(ModManifest mod, ModManifest.Version version);
}
