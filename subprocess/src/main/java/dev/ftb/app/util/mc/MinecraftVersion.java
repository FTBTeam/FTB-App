package dev.ftb.app.util.mc;

import org.semver4j.Semver;

public record MinecraftVersion(
    int major,
    int minor,
    int patch,

    String version,
    int javaVersion,

    int year,
    int week,
    long releaseDate,

    Semver semver
) {
    public static MinecraftVersion create(int major, int minor, int patch, String version, int javaVersion, int year, int week, long releaseDate) {
        return new MinecraftVersion(major, minor, patch, version, javaVersion, year, week, releaseDate, new Semver("%s.%s.%s".formatted(major, minor, patch)));
    }
}
