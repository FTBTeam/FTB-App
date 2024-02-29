package net.creeperhost.creeperlauncher.storage;

public record UserApiCredentials(
    String apiUrl,
    String apiSecret,
    boolean supportsPublicPrefix,
    boolean usesBearerAuth
) {
}
