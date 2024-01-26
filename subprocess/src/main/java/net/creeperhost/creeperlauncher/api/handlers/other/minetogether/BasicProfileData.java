package net.creeperhost.creeperlauncher.api.handlers.other.minetogether;

import org.jetbrains.annotations.Nullable;

public record BasicProfileData(String uuid, String modpacksToken, @Nullable S3Credentials s3Credentials) {}
