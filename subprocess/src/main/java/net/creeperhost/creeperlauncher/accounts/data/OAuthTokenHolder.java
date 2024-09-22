package net.creeperhost.creeperlauncher.accounts.data;

public record OAuthTokenHolder(
    String accessToken,
    String refreshToken,
    long expires
) {}
