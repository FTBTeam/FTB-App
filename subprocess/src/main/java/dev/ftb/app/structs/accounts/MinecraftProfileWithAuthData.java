package dev.ftb.app.structs.accounts;

public record MinecraftProfileWithAuthData(
    MinecraftProfileData profileData,
    String minecraftAccessToken,
    int minecraftExpiresIn,
    OAuthTokenHolder microsoftToken,
    String userHash,
    String notAfter,
    String xstsToken
) { }
