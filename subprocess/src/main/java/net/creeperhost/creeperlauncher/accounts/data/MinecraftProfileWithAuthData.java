package net.creeperhost.creeperlauncher.accounts.data;

public record MinecraftProfileWithAuthData(
    MinecraftProfileData profileData,
    String minecraftAccessToken,
    int minecraftExpiresIn,
    OAuthTokenHolder microsoftToken,
    String userHash,
    String notAfter,
    String xstsToken
) { }
