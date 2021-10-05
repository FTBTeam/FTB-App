package net.creeperhost.creeperlauncher.mod;

import com.google.common.hash.HashCode;
import com.google.gson.JsonSyntaxException;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.api.DownloadableFile;
import net.creeperhost.creeperlauncher.api.handlers.ModFile;
import net.creeperhost.creeperlauncher.pack.IPack;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.LoaderTarget;
import net.creeperhost.creeperlauncher.util.WebUtils;

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

        public DownloadableFile getDownloadableFile(IPack instance) {
            ArrayList<HashCode> hashes = new ArrayList<>();

            if (!sha1.isEmpty()) {
                HashCode code = HashCode.fromString(sha1);
                hashes.add(code);
            }

            return new DownloadableFile(version, instance.getDir().resolve(path).resolve(name), url, hashes, size, id, name, type, updated);
        }

        public List<Version> getDependencies(List<ModFile> existingFiles, List<Version> fileCandidates, LoaderTarget gameTarget, LoaderTarget loaderTarget) {
            if (fileCandidates == null) {
                fileCandidates = new ArrayList<>();
                fileCandidates.add(this);
            }
            ArrayList<Version> dependTemp = new ArrayList<>();
            dependLoop:
            for (Dependency dependInt: dependencies) {
                if(!dependInt.required) continue;
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
            Optional<ModFile> first = existingFiles.stream().filter(mod -> mod.getName().equals(tempVer.name)).findFirst();
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
        String resp = WebUtils.getAPIResponse(Constants.MOD_API + id);
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

    /*targets.stream().filter(target -> LoaderTarget.type.equals("game")).sorted((a, b) -> {
        String aStr = a.version;
        String bStr = b.version;
        if (a.equals(b)) return 0;
        String aOnlyNumbers = aStr.replaceAll("[^0-9.]", "");
        String bOnlyNumbers = bStr.replaceAll("[^0-9.]", "");
        String[] splita = aOnlyNumbers.split("\\.");
        String[] splitb = bOnlyNumbers.split("\\.");
        for (int i = 0, splitaLength = splita.length; i < splitaLength; i++) {
            if(i > splitb.length - 1) return 1;
            int partIntA = -1;
            int partIntB = -1;
            try {
                partIntA = Integer.parseInt(splita[i]);
            } catch (Throwable ignored) {}
            try {
                partIntB = Integer.parseInt(splitb[i]);
            } catch (Throwable ignored) {}
            int res = Integer.compare(partIntA, partIntB);
            if (res != 0) return res;
        }
        return splitb.length > splita.length ? -1 : 0;
    }).forEach(System.out::println);*/ // commented as realised I don't really need it as I already have a LoaderTarget in the pack
}
