package net.creeperhost.creeperlauncher.mod;

import com.google.common.hash.HashCode;
import com.google.gson.JsonSyntaxException;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.api.DownloadableFile;
import net.creeperhost.creeperlauncher.api.handlers.ModFile;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.LoaderTarget;
import net.creeperhost.creeperlauncher.util.WebUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Mod {
    public int id;
    String name;
    int installs;
    String status;
    String message;
    ArrayList<Version> versions;

    public static class Version {
        String version;
        String path;
        String name;
        String url;
        String sha1;
        String updated;
        long size;
        boolean clientOnly;
        private long id;
        String type;
        ArrayList<LoaderTarget> targets;
        ArrayList<Dependency> dependencies;

        private transient Mod parentMod;
        public transient boolean existsOnDisk;

        public DownloadableFile getDownloadableFile(Path instance) {
            ArrayList<HashCode> hashes = new ArrayList<>();

            if (!sha1.isEmpty()) {
                HashCode code = HashCode.fromString(sha1);
                hashes.add(code);
            }

            return new DownloadableFile(instance.resolve(path).resolve(name), url, hashes, size, id, name, type);
        }

        public List<Version> getDependencies(List<ModFile> existingFiles, List<Version> fileCandidates, LoaderTarget gameTarget, LoaderTarget loaderTarget) {
            if (fileCandidates == null) {
                fileCandidates = new ArrayList<>();
                fileCandidates.add(this);
            }
            ArrayList<Version> dependTemp = new ArrayList<>();
            dependLoop:
            for (Dependency dependInt: dependencies) {
                if (!dependInt.required) continue;
                if (existingFiles.stream().anyMatch((ModFile f) -> f.getCurseProject() == dependInt.id)) continue;
                for(Version candVer: fileCandidates) {
                    if (candVer.parentMod.id == dependInt.id) {
                        continue dependLoop;
                    }
                }
                Mod otherMod = getFromAPI(dependInt.id);
                if (otherMod != null) {
                    Version versionMatching = otherMod.findVersionMatching(existingFiles, gameTarget, loaderTarget);
                    if (versionMatching != null) {
                        dependTemp.add(versionMatching);
                        dependTemp.addAll(versionMatching.getDependencies(existingFiles, fileCandidates, gameTarget, loaderTarget));
                    }
                }
            }
            return dependTemp;
        }

        public void setParentMod(Mod mod) {
            parentMod = mod;
        }

        public Mod getParentMod() {
            return parentMod;
        }

        public String getName() {
            return name;
        }
    }

    private Version findVersionMatching(List<ModFile> existingFiles, LoaderTarget gameTarget, LoaderTarget loaderTarget) {
        for (Version tempVer: versions) {
            Optional<ModFile> first = existingFiles.stream().filter(mod -> mod.getRealName().equals(tempVer.name)).findFirst();
            if (first.isPresent()) {
                tempVer.existsOnDisk = true;
                tempVer.parentMod = this;
                return tempVer;
            }

            Optional<LoaderTarget> gameTar = tempVer.targets.stream().filter(tar -> tar.equalsNoVersion(gameTarget)).findFirst();
            Optional<LoaderTarget> modloaderTar = tempVer.targets.stream().filter(tar -> tar.equalsNoVersion(loaderTarget)).findFirst();

            if (gameTar.isPresent() && modloaderTar.isPresent()) {
                tempVer.parentMod = this;
                return tempVer;
            }
        }

        return null;
    }

    public Version getVersion(int id) {
        for (Version version: versions) {
            if (version.id == id) {
                version.setParentMod(this);
                return version;
            }
        }
        return null;
    }

    public String getName() {
        return this.name;
    }

    public static Mod getFromAPI(int id) {
        String resp = WebUtils.getAPIResponse(Constants.getModEndpoint() + id);
        try {
            Mod mod = GsonUtils.GSON.fromJson(resp, Mod.class);
            if (mod.status.equals("error")) {
                return null;
            }
            return mod;
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    public static class Dependency {
        public int id;
        public boolean required;
    }

}
