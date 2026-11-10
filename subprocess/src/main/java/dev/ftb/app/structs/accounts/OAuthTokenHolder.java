package dev.ftb.app.structs.accounts;

public record OAuthTokenHolder(
    String accessToken,
    String refreshToken,
    long expires
) {}
