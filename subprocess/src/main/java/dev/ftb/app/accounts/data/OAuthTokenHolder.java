package dev.ftb.app.accounts.data;

public record OAuthTokenHolder(
    String accessToken,
    String refreshToken,
    long expires
) {}
