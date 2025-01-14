package dev.ftb.app.storage;

public record UserApiCredentials(
    String apiUrl,
    String apiSecret,
    boolean supportsPublicPrefix,
    boolean usesBearerAuth
) {
}
