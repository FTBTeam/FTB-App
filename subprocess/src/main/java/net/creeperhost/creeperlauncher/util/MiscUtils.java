package net.creeperhost.creeperlauncher.util;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.os.OS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiscUtils
{
    private static final Logger LOGGER = LogManager.getLogger();
    public static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public static CompletableFuture<?> allFutures(ArrayList<CompletableFuture<?>> futures)
    {
        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])).exceptionally((t) ->
                {
                    LOGGER.warn("Future failed.", t);
                    return null;
                }
        );
        futures.forEach((x) ->
        {
            x.exceptionally((t) ->
            {
                combinedFuture.completeExceptionally(t);
                return null;
            });
        });
        return combinedFuture;
    }
    public static int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public static long unixtime()
    {
        return System.currentTimeMillis() / 1000L;
    }

    public static String getDateAndTime()
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
        LocalDateTime now = LocalDateTime.now();

        return dateTimeFormatter.format(now);
    }

    private static String[] javaRegLocationsAdoptOpenJDK = new String[] {"SOFTWARE\\AdoptOpenJDK\\JDK", "SOFTWARE\\AdoptOpenJDK\\JRE"};
    private static String[] javaRegLocationsOracle = new String[] {"SOFTWARE\\JavaSoft\\Java Development Kit", "SOFTWARE\\JavaSoft\\Java Runtime Environment"};
    private static String[] linuxJavaPathLocations = new String[] {"/usr/lib/jvm", "/usr/java"};

    public static void updateJavaVersions()
    {
        HashMap<String, String> versions = new HashMap<>();
        switch(OS.CURRENT) {
            case WIN:
                versions.put("Mojang Built-in", "");
                for (String location : javaRegLocationsOracle)
                {
                    if (Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, location))
                    {
                        WinReg.HKEYByReference key = Advapi32Util.registryGetKey(WinReg.HKEY_LOCAL_MACHINE, location, WinNT.KEY_READ);
                        String[] children = Advapi32Util.registryGetKeys(key.getValue());
                        for (String child : children) {
                            String javaHome = null;
                            try {
                                javaHome = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, location + "\\" + child, "JavaHome");
                            } catch (Win32Exception ignored) {
                            }
                            if (javaHome != null) {
                                versions.put("Oracle " + child + " " + (location.contains("Development") ? "JDK" : "JRE") + " (64bit)", Path.of(javaHome, "bin", "java.exe").toString());
                            }
                        }
                    }
                }
                for (String location : javaRegLocationsAdoptOpenJDK)
                {
                    if (Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, location))
                    {
                        WinReg.HKEYByReference key = Advapi32Util.registryGetKey(WinReg.HKEY_LOCAL_MACHINE, location, WinNT.KEY_READ);
                        String[] children = Advapi32Util.registryGetKeys(key.getValue());
                        for (String child : children) {
                            String javaHome = null;
                            WinReg.HKEYByReference keyTypes = Advapi32Util.registryGetKey(WinReg.HKEY_LOCAL_MACHINE, location + "\\" + child, WinNT.KEY_READ);
                            String[] childrenTypes = Advapi32Util.registryGetKeys(keyTypes.getValue());
                            for (String childType : childrenTypes) {
                                try {
                                    javaHome = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, location + "\\" + child + "\\" + childType + "\\MSI", "Path");
                                } catch (Win32Exception ignored) {
                                }
                                if (javaHome != null) {
                                    versions.put("AdoptOpenJDK " + child + "_" + childType + " " + location.substring(location.lastIndexOf("\\") + 1) + " (64bit)", Path.of(javaHome, "bin", "java.exe").toString());
                                }
                            }
                        }
                    }
                }
                Path java = findExecutableOnPath("java.exe");
                if (java != null)
                {
                    String ver = versionFromFile(java);
                    if (!ver.equals("Unknown")) versions.put(ver, java.toAbsolutePath().toString());
                }
                break;
            case MAC:
                versions.put("Mojang Built-in", "");
                Path javaMac = findExecutableOnPath("java");
                if (javaMac != null)
                {
                    String ver = versionFromFile(javaMac);
                    if (!ver.equals("Unknown")) versions.put(ver, javaMac.toAbsolutePath().toString());
                }
                break;
            case LINUX:
                versions = scanPath(linuxJavaPathLocations);
                Path javaLinux = findExecutableOnPath("java");
                if (javaLinux != null)
                {
                    String ver = versionFromFile(javaLinux);
                    if (!ver.equals("Unknown")) versions.put(ver, javaLinux.toAbsolutePath().toString());
                }
                break;
            case UNKNOWN:
                break;
        }

        CreeperLauncher.javaVersions = versions;
    }

    private static HashMap<String, String> scanPath(String[] paths)
    {
        HashMap<String, String> versions = new HashMap<>();
        for (String location : paths) {
            Path path = Path.of(location);
            if(!Files.isDirectory(path)) continue;
            List<Path> files = FileUtils.listDir(path);
            if (files == null) continue;
            for(Path javaDir : files)
            {
                javaDir = unsymlink(path);
                if (javaDir == null) continue;
                String javaVersionName = javaDir.getFileName().toString();
                if (!javaVersionName.matches("(.*)\\d+(.*)")) continue;
                Path javaExe = javaDir.resolve("bin/java");
                if (Files.notExists(javaExe) || !Files.isExecutable(javaExe)) javaExe = javaDir.resolve("bin/java.exe");
                if (Files.notExists(javaExe) || !Files.isExecutable(javaExe)) continue;
                versions.put(javaVersionName, javaExe.toAbsolutePath().toString());
            }
        }
        return versions;
    }

    private static Path unsymlink(Path file) {
        int i = 0;
        for(; i < 5 && Files.isSymbolicLink(file); i++)
        {
            try {
                file = file.normalize().toRealPath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (i == 5 && Files.isSymbolicLink(file)) return null;
        return file;
    }

    private static String versionFromFile(Path file) {
        Path unsymlink = unsymlink(file);
        if (unsymlink != null) file = unsymlink;
        Path parent = file.getParent();
        if (!parent.getFileName().toString().equals("bin")) return "Unknown";
        Path versionDir = file.getParent();
        String version = versionDir.getFileName().toString();
        if (version.matches("(.*)\\d+(.*)")) return version;
        return versionFromExecutable(file);
    }

    private static Path findExecutableOnPath(String name) {
        for (String dirname : System.getenv("PATH").split(File.pathSeparator)) {
            Path file = Path.of(dirname).resolve(name);
            if (Files.isRegularFile(file) && Files.isExecutable(file)) {
                return file;
            }
        }
        return null;
    }

    private static String execAndFullOutput(String ...args) {
        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
        builder.command(args);
        try {
            Process start = builder.start();
            byte[] bytes = IOUtils.toByteArray(start.getInputStream());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static final Pattern p = Pattern.compile("\\w+ version \"(.*?)\"");

    private static String versionFromExecutable(Path path)
    {
        String execResult = execAndFullOutput(path.toAbsolutePath().toString(), "-version");
        Matcher m = p.matcher(execResult);
        if (m.find())
        {
            return m.group(1);
        }
        return "Unknown";
    }

    public static boolean isInt(String in){
        try {
            Integer.parseInt(in);
        }catch(Exception ignored) {
            return false;
        }
        return true;
    }
}
