package dev.ftb.app.util.mc;

import java.util.List;

public record MinecraftVersionsResponse(
    Latest latest,
    List<Version> versions
) {
    public record Latest(
        String release,
        String snapshot
    ) {}

    public record Version(
        int complianceLevel,
        String id,
        String releaseTime,
        String sha1,
        String time,
        String type,
        String url
    ) {}
}