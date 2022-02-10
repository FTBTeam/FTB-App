package net.creeperhost.creeperlauncher.minecraft.jsons;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.quack.gson.HashCodeAdapter;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.gson.LowerCaseEnumAdapterFactory;
import net.covers1624.quack.gson.MavenNotationAdapter;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.platform.OperatingSystem;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask.DownloadValidation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.creeperhost.creeperlauncher.Constants.CH_MAVEN;
import static org.apache.commons.lang3.StringUtils.removeStart;

/**
 * Version manifest for a given game version.
 * <p>
 * Created by covers1624 on 8/11/21.
 */
@SuppressWarnings ("NotNullFieldNotInitialized")
public class VersionManifest {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG = Boolean.getBoolean("VersionManifest.debug");

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(OS.class, new OsDeserializer())
            .registerTypeAdapter(HashCode.class, new HashCodeAdapter())
            .create();

    private static final Rule WIN_10_RULE = new Rule(
            Action.ALLOW,
            new OSRule(
                    OS.WINDOWS,
                    "^10\\\\.",
                    null
            ),
            null
    );

    public String id;
    @Nullable
    public Arguments arguments;
    public AssetIndex assetIndex;
    public String assets;
    public int complianceLevel;
    public Map<String, Download> downloads = new HashMap<>();
    @Nullable
    public JavaVersionJson javaVersion;
    public List<Library> libraries = new ArrayList<>();
    @Nullable
    public Logging logging;
    @Nullable
    public String mainClass;
    @Nullable
    public String minecraftArguments;
    public int minimumLauncherVersion;
    @Nullable
    public Date time;
    @Nullable
    public Date releaseTime;
    @Nullable
    public String type;
    @Nullable
    public String inheritsFrom;

    /**
     * Updates (if required) the specified version manifest.
     *
     * @param versionsDir The versions directory.
     * @param version     The {@link VersionListManifest.Version} to update.
     * @return The {@link VersionManifest} parsed from disk.
     * @throws IOException        Thrown when an error occurs whilst loading the manifest.
     * @throws JsonParseException Thrown when the Json cannot be parsed.
     */
    public static VersionManifest update(Path versionsDir, VersionListManifest.Version version) throws IOException {
        Path versionFile = versionsDir.resolve(version.id).resolve(version.id + ".json");
        LOGGER.info("Updating version manifest for '{}' from '{}'.", version.id, version.url);
        NewDownloadTask downloadTask = NewDownloadTask.builder()
                .url(version.url)
                .dest(versionFile)
                .withValidation(DownloadValidation.of()
                        .withUseETag(true)
                        .withUseOnlyIfModified(true)
                )
                .build();

        if (!downloadTask.isRedundant()) {
            try {
                downloadTask.execute(null, null);
            } catch (Throwable e) {
                if (Files.exists(versionFile)) {
                    LOGGER.warn("Failed to update VersionManifest. Continuing with disk cache..", e);
                } else {
                    throw new IOException("Failed to update VersionManifest. Disk cache does not exist.", e);
                }
            }
        }
        return JsonUtils.parse(GSON, versionFile, VersionManifest.class);
    }

    public Stream<Library> getLibraries(Set<String> features) {
        return libraries.stream()
                .filter(e -> e.apply(features));
    }

    @Nullable
    @Contract ("!null->!null")
    public JavaVersion getJavaVersionOrDefault(@Nullable JavaVersion _default) {
        if (javaVersion == null) return _default;
        JavaVersion parse = JavaVersion.parse(String.valueOf(javaVersion.majorVersion));
        if (parse == null || parse == JavaVersion.UNKNOWN) {
            LOGGER.error("Unable to parse '{}' into a JavaVersion.", javaVersion.majorVersion);
            return _default;
        }
        return parse;
    }

    @Nullable
    public NewDownloadTask getClientDownload(Path versionsDir) {
        Download download = downloads.get("client");
        if (download == null || download.url == null) return null;

        DownloadValidation validation = DownloadValidation.of()
                .withExpectedSize(download.size);
        if (download.sha1 != null) {
            validation = validation.withHash(Hashing.sha1(), download.sha1);
        }
        return NewDownloadTask.builder()
                .url(download.url)
                .dest(versionsDir.resolve(id).resolve(id + ".jar"))
                .withValidation(validation)
                .build();
    }

    public static List<String> collectJVMArgs(List<VersionManifest> manifests, Set<String> features) {
        // If any manifests have the 'arguments' field, then we exclusively use that
        //  this maintains the semantics of the vanilla launcher.
        if (manifests.stream().anyMatch(e -> e.arguments != null)) {
            return manifests.stream()
                    .map(e -> e.arguments)
                    .filter(e -> e != null && e.jvm != null)
                    .flatMap(e -> e.jvm.stream().flatMap(eVal -> eVal.eval(features).stream()))// Evaluate all arguments.
                    .collect(Collectors.toList());
        }

        List<String> ret = new LinkedList<>();
        if (OperatingSystem.current().isWindows()) {
            ret.add("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump");
            if (WIN_10_RULE.apply(features) == Action.ALLOW) {
                ret.add("-Dos.name=Windows 10");
                ret.add("-Dos.version=10.0");
            }
        }
        if (OperatingSystem.current().isMacos()) {
            ret.add("-Xdock:name=Minecraft");
            ret.add("-Xdock:icon=${minecraft_icon}");
        }

        ret.add("-Djava.library.path=${natives_directory}");
        ret.add("-Dminecraft.launcher.brand=${launcher_name}");
        ret.add("-Dminecraft.launcher.version=${launcher_version}");
        ret.add("-Dminecraft.client.jar=${primary_jar}");
        ret.add("-cp");
        ret.add("${classpath}");
        return ret;
    }

    public static List<String> collectProgArgs(List<VersionManifest> manifests, Set<String> features) {
        // If any manifests have the 'arguments' field, then we exclusively use that
        //  this maintains the semantics of the vanilla launcher.
        if (manifests.stream().anyMatch(e -> e.arguments != null)) {
            return manifests.stream()
                    .map(e -> e.arguments)
                    .filter(e -> e != null && e.game != null)
                    .flatMap(e -> e.game.stream().flatMap(eVal -> eVal.eval(features).stream()))// Evaluate all arguments.
                    .collect(Collectors.toList());
        }
        for (VersionManifest manifest : Lists.reverse(manifests)) {
            if (manifest.minecraftArguments != null) {
                List<String> args = Lists.newArrayList(manifest.minecraftArguments.split(" "));
                if (features.contains("is_demo_user")) {
                    args.add("--demo");
                }
                if (features.contains("has_custom_resolution")) {
                    args.add("--width");
                    args.add("${resolution_width}");
                    args.add("--height");
                    args.add("${resolution_height}");
                }
                return args;
            }
        }
        return Collections.emptyList();
    }

    public static class Arguments {

        @Nullable
        @JsonAdapter (ArgListDeserializer.class)
        public List<EvalValue> game;
        @Nullable
        @JsonAdapter (ArgListDeserializer.class)
        public List<EvalValue> jvm;
    }

    public static class AssetIndex {

        public String id;
        @JsonAdapter (HashCodeAdapter.class)
        public HashCode sha1;
        public int size;
        public int totalSize;
        public String url;
    }

    public static class Download {

        @Nullable
        @JsonAdapter (HashCodeAdapter.class)
        public HashCode sha1;
        public int size;
        @Nullable
        public String url;
    }

    public static class JavaVersionJson {

        public String component;
        public int majorVersion;
    }

    public static class Library {

        @JsonAdapter (MavenNotationAdapter.class)
        public MavenNotation name;
        @Nullable
        public Extract extract;
        @Nullable
        public Downloads downloads;
        @Nullable
        public List<Rule> rules;
        @Nullable
        public Map<OS, String> natives;
        @Nullable
        public String url;

        public boolean apply(Set<String> features) {
            return Rule.apply(rules, features);
        }

        @Nullable
        public NewDownloadTask createDownloadTask(Path librariesDir, boolean ignoreLocalLibraries) {
            if (url != null || downloads == null) {
                // The Vanilla launcher will explicitly use the 'url' property if it exists, however we override this to the CH maven.
                // If the 'downloads' property is null, it tries from Mojang's maven directly, however we override this to the CH maven.
                return NewDownloadTask.builder()
                        .url(CH_MAVEN + name.toPath())
                        .dest(name.toPath(librariesDir))
                        .build();
            }

            // We have a 'downlaods' property, but no 'natives'.
            if (natives == null) {
                if (downloads.artifact == null) return null;
                return downloadTaskFor(librariesDir, downloads.artifact, name, ignoreLocalLibraries);
            }
            // We have natives.
            if (downloads.classifiers == null) return null; // What? but okay, just ignore.
            String classifier = natives.get(OS.current());
            if (classifier == null) return null; // No natives for this platform.
            LibraryDownload artifact = downloads.classifiers.get(classifier);
            if (artifact == null) return null; // Shouldn't happen, but okay.
            return downloadTaskFor(librariesDir, artifact, name.withClassifier(classifier), ignoreLocalLibraries);
        }

        @Nullable
        private NewDownloadTask downloadTaskFor(Path librariesDir, LibraryDownload artifact, MavenNotation name, boolean ignoreLocalLibraries) {
            if (StringUtils.isEmpty(artifact.url) && ignoreLocalLibraries) return null; // Ignore. Library is not a remote resource. TODO, these should still be validated though.

            if (artifact.path == null) {
                LOGGER.warn("Artifact has null path? Nani?? Skipping.. {}", name);
                return null;
            }

            DownloadValidation validation = DownloadValidation.of()
                    .withExpectedSize(artifact.size);
            if (artifact.sha1 != null) {
                validation = validation.withHash(Hashing.sha1(), artifact.sha1);
            }

            // Dumb hack for Forge/Mojang as Mojang doesnt support classifiers in maven coords.
            String url = artifact.url;
            int startIdx =url.indexOf(name.toModulePath());
            if (startIdx != -1) {
                url = CH_MAVEN + removeStart(url.substring(startIdx), "/");
            }
            if (url.isEmpty()) {
                url = CH_MAVEN + removeStart(artifact.path, "/");
            }

            // Build the URL ourselves to use the CH maven instead of the provided 'url' attribute
            return NewDownloadTask.builder()
                    .url(url)
                    .dest(librariesDir.resolve(artifact.path))
                    .withValidation(validation)
                    .build();
        }
    }

    public static class Logging {

        @Nullable
        public LoggingEntry client;
    }

    public static class LoggingEntry {

        @Nullable
        public String argument;
        @Nullable
        public String type;
        @Nullable
        public LoggingDownload file;
    }

    public static class LoggingDownload extends Download {

        @Nullable
        public String id;
    }

    public static class Extract {

        @Nullable
        public List<String> exclude;

        public boolean shouldExtract(String name) {
            if (exclude == null) return true;
            for (String s : exclude) {
                if (name.startsWith(s)) return false;
            }
            return true;
        }
    }

    public static class LibraryDownload extends Download {

        @Nullable
        public String path;
    }

    public static class Downloads {

        @Nullable
        public LibraryDownload artifact;
        @Nullable
        public Map<String, LibraryDownload> classifiers;
    }

    public static class Rule {

        @Nullable
        @JsonAdapter (LowerCaseEnumAdapterFactory.class)
        public Action action;
        @Nullable
        public OSRule os;
        @Nullable
        public Map<String, Boolean> features;

        public Rule() {
        }

        public Rule(Action action, @Nullable OSRule os, @Nullable Map<String, Boolean> features) {
            this();
            this.action = action;
            this.os = os;
            this.features = features;
        }

        // Returns null for no-preference.
        @Nullable
        public Action apply(Set<String> features) {
            if (os != null && !os.applies()) {
                // Os doesn't match. No preference for this rule.
                return null;
            }

            if (this.features != null) {
                for (Map.Entry<String, Boolean> feature : this.features.entrySet()) {
                    if (features.contains(feature.getKey()) != feature.getValue()) {
                        // Feature not enabled. No preference for this rule.
                        return null;
                    }
                }
            }

            assert action != null;
            // Default action.
            return action;
        }

        public static boolean apply(@Nullable List<Rule> rules, Set<String> features) {
            if (rules == null) return true;

            return rules.stream()
                    .map(e -> e.apply(features))
                    // This mirrors Mojang's logic, Last to return no-preference, takes precedence.
                    .reduce(Action.DISALLOW, (a, b) -> b != null ? b : a) == Action.ALLOW;
        }
    }

    public static class OSRule {

        @Nullable
        public OS name;
        @Nullable
        public String version;
        @Nullable
        public String arch;

        public OSRule() {
        }

        public OSRule(@Nullable OS name, @Nullable String version, @Nullable String arch) {
            this();
            this.name = name;
            this.version = version;
            this.arch = arch;
        }

        public boolean applies() {
            OS current = OS.current();
            if (name != null && name != current) return false;
            if (version != null) {
                try {
                    Pattern pattern = Pattern.compile(version);
                    if (!pattern.matcher(System.getProperty("os.version")).matches()) {
                        return false;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (arch != null) {
                try {
                    Pattern pattern = Pattern.compile(arch);
                    if (!pattern.matcher(System.getProperty("os.arch")).matches()) {
                        return false;
                    }
                } catch (Throwable ignored) {
                }
            }
            return true;
        }
    }

    public enum OS {
        WINDOWS,
        LINUX,
        OSX,
        UNKNOWN;

        @Nullable
        private static OS current;

        public static OS current() {
            if (current != null && !DEBUG) return current;
            current = parse(System.getProperty("os.name"));
            return current;
        }

        static OS parse(String name) {
            name = name.toLowerCase(Locale.ROOT);
            if (name.contains("win")) {
                return WINDOWS;
            }
            if (name.contains("mac") || name.contains("osx")) {
                return OSX;
            }
            if (name.contains("linux")) {
                return LINUX;
            }
            return UNKNOWN;
        }
    }

    public enum Action {
        ALLOW,
        DISALLOW,
    }

    public static class EvalValue {

        private final List<String> values;

        public EvalValue(List<String> values) {
            this.values = values;
        }

        public List<String> eval(Set<String> feature) {
            return values;
        }
    }

    public static class RuledEvalValue extends EvalValue {

        private final List<Rule> rules;

        public RuledEvalValue(List<String> values, List<Rule> rules) {
            super(values);
            this.rules = rules;
        }

        @Override
        public List<String> eval(Set<String> feature) {
            if (!Rule.apply(rules, feature)) return List.of();

            return super.eval(feature);
        }
    }

    public static class ArgListDeserializer implements JsonDeserializer<List<EvalValue>> {

        private static final Type RULES_LIST = new TypeToken<List<Rule>>() { }.getType();

        @Override
        public List<EvalValue> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<EvalValue> evalValues = new ArrayList<>();
            List<String> list = new ArrayList<>();
            if (!json.isJsonArray()) throw new JsonParseException("Expected JsonArray. '" + json + "'");
            for (JsonElement elm : json.getAsJsonArray()) {
                if (elm.isJsonPrimitive()) {
                    list.add(elm.getAsString());
                } else if (elm.isJsonObject()) {
                    evalValues.add(new EvalValue(List.copyOf(list)));
                    list.clear();

                    JsonObject obj = elm.getAsJsonObject();
                    if (!obj.has("value")) throw new JsonParseException("Missing 'value' element. '" + obj + "'");
                    JsonElement value = obj.get("value");
                    if (!value.isJsonPrimitive() && !value.isJsonArray()) throw new JsonParseException("Expected JsonPrimitive or JsonArray. '" + value + "'");
                    List<String> values = value.isJsonPrimitive() ? List.of(value.getAsString()) : getStringList(value.getAsJsonArray());
                    if (!obj.has("rules")) throw new JsonParseException("Missing 'rules' element. '" + obj + "'");

                    List<Rule> rules = context.deserialize(obj.get("rules"), RULES_LIST);
                    evalValues.add(new RuledEvalValue(values, rules));
                }
            }
            if (!list.isEmpty()) {
                evalValues.add(new EvalValue(List.copyOf(list)));
            }
            return evalValues;
        }

        private static List<String> getStringList(JsonArray array) {
            List<String> values = new ArrayList<>();
            for (JsonElement elm : array) {
                if (!elm.isJsonPrimitive()) throw new JsonParseException("Expected JsonPrimitive '" + array + "'");
                values.add(elm.getAsString());
            }
            return List.copyOf(values);
        }
    }

    public static class OsDeserializer implements JsonDeserializer<OS> {

        @Override
        public OS deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonPrimitive()) throw new JsonParseException("Expected json primitive. Got: " + json);
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (!primitive.isString()) throw new JsonParseException("Expected json string. Got:" + primitive);
            OS os = OS.parse(primitive.getAsString());
            if (os == OS.UNKNOWN) throw new JsonParseException("Could not parse OS from String: " + primitive.getAsString());
            return os;
        }
    }
}
