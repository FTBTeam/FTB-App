package dev.ftb.app.util.mc;

import com.google.gson.Gson;
import dev.ftb.app.Constants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static dev.ftb.app.Constants.MC_VERSIONS;

/**
 * A hybrid system for fetching and storing Minecraft versions in an ordered, dated & snapshot friendly manner.
 * 
 * When online, we'll update our own internal list of Minecraft versions, but when offline, we'll use the last known list.
 */
public enum MinecraftVersions {
    INSTANCE;
    
    private static final Logger logger = LoggerFactory.getLogger(MinecraftVersions.class);
    
    // Populated with the default versions.
    private final List<MinecraftVersion> versions = new ArrayList<>(MinecraftVersionPresets.VALUES.stream().map(MinecraftVersionPresets::getVersionData).toList());
    
    public void init() {
        try {
            fetchVersions();
        } catch (Exception e) {
            logger.error("Failed to fetch Minecraft versions", e);
        }
    }
    
    public void fetchVersions() {
        CompletableFuture.runAsync(() -> {
            OkHttpClient client = Constants.httpClient();
            try (var response = client.newCall(new Request.Builder().url(MC_VERSIONS).build()).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new RuntimeException("Failed to fetch Minecraft versions: " + response.code());
                }
                
                var json = new Gson().fromJson(response.body().string(), MinecraftVersionsResponse.class);
                parseAndImportVersions(json);

                logger.info("Successfully fetched Minecraft versions");
                logger.info("Found {} versions", versions.size());
            } catch (Exception e) {
                logger.error("Failed to fetch Minecraft versions", e);
            }
        });
    }

    @Nullable
    public MinecraftVersion parse(String version) {
        // Non-special versions are the simplest to parse
        if (version.startsWith("1.")) {
            // This should be super simple to find.
            var parts = version.split("\\.");

            if (parts.length == 2) {
                parts = new String[] {parts[0], parts[1].split("-")[0], "0"};
            } else {
                parts[2] = parts[2].split("-")[0];
            }

            var numberParts = Arrays.stream(parts).map(Integer::parseInt).toArray(Integer[]::new);
            var major = numberParts[0];
            var minor = numberParts[1];
            var patch = numberParts[2];

            return versions.stream().filter(v -> v.major() == major && v.minor() == minor && v.patch() == patch).findFirst().orElse(null);
        }

        // We're likely a snapshot, if we're a snapshot, we'll find the closest version to the snapshot.
        if (version.contains("w")) {
            return findClosesVersionFromSnapshot(version);
        }

        // We have no idea what this is.
        return null;
    }

    private MinecraftVersion findClosesVersionFromSnapshot(String snapshot) {
        if (!snapshot.contains("w")) {
            throw new IllegalArgumentException("Invalid snapshot version: " + snapshot);
        }

        // Let's split this up and find the closest version.
        // YYwWW(a/b/c) We only care about the YY and WW. So slicing the string will be fine.
        var year = Integer.parseInt(snapshot.substring(0, 2));
        var week = Integer.parseInt(snapshot.substring(3, 5));

        // Now we need to find the closest version to this snapshot.
        // If we have, for example, 24w46a, we should end up finding 1.21.4 as it's week and year should be the closest 
        // when compared to the other versions.

        // If we work from the oldest version to the newest, we should keep bumping the version until 
        // we pass the snapshot's week and year, this will give us the correct version.
        MinecraftVersion closest = null;
        boolean found = false;
        for (MinecraftVersion version : versions) {
            if (closest == null) {
                closest = version;
                continue;
            }

            closest = version;

            if (version.year() > year || (version.year() == year && version.week() > week)) {
                found = true;
                break;
            }
        }

        if (!found) {
            return versions.getLast();
        }

        return closest;
    }
    
    private void parseAndImportVersions(MinecraftVersionsResponse response) {
        var versions = response.versions();
        
        // Reverse the list
        versions.sort(Comparator.comparing(MinecraftVersionsResponse.Version::releaseTime));
        
        boolean passedFirstVersion = false;
        MinecraftJavaBump currentBump = MinecraftJavaBump.JAVA_8;
        for (var version : versions) {
            // Skip random version
            if (version.id().equals("1.RV-Pre1")) continue;
            
            if (!passedFirstVersion && version.id().equals("1.0")) {
                passedFirstVersion = true;
            }
            
            if (!passedFirstVersion) {
                continue;
            }

            if (version.id().contains("w") || version.id().contains("-pre") || version.id().contains(" Pre-") || version.id().contains("-rc") || version.id().startsWith("b") || version.id().startsWith("a")) {
                // We're in a snapshot, we don't record these but we do need to watch for the next jump.
                currentBump = jumpJavaVersion(version.id(), currentBump);
                continue;
            }

            var parts = version.id().split("-")[0].split("\\.");
            var major = Integer.parseInt(parts[0]);
            var minor = Integer.parseInt(parts[1]);
            var patch = 0;
            if (parts.length > 2) {
                patch = Integer.parseInt(parts[2]);
            }
            
            // Read the ISO date and convert to unix time
            Instant releaseTime = Instant.parse(version.releaseTime());
            LocalDateTime date = LocalDateTime.ofInstant(releaseTime, ZoneId.of("UTC"));
            
            var finalPatch = patch;
            var existing = this.versions.stream().filter(v -> v.major() == major && v.minor() == minor && v.patch() == finalPatch).findFirst();
            if (existing.isPresent()) {
                continue; // We don't need duplicates.
            }

            var year = Integer.parseInt(date.format(DateTimeFormatter.ofPattern("yy")));
            var week = date.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());

            this.versions.add(MinecraftVersion.create(major, minor, patch, version.id(), currentBump.getJavaVersion(), year, week, releaseTime.getEpochSecond()));

            currentBump = jumpJavaVersion(version.id(), currentBump);
        }
    }
    
    private static MinecraftJavaBump jumpJavaVersion(String mcVersion, MinecraftJavaBump currentBump) {
        if (currentBump.getUntilVersion().equals(MinecraftJavaBump.MAGIC_SKIP_VERSION)) {
            return currentBump;
        }
        
        if (mcVersion.equals(currentBump.getUntilVersion())) {
            // Get the next version
            return MinecraftJavaBump.values()[currentBump.ordinal() + 1];
        }
        
        return currentBump;
    }

    public List<MinecraftVersion> versions() {
        return versions;
    }
}
