package dev.ftb.app.structs.accounts;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

/// The token response from the FTB identity server (Keycloak) once the device flow completes.
///
/// `idToken` is the one the modpack API accepts, as it's issued to `ftb-app`. With `offline_access` the refresh token
/// doesn't expire and `refreshExpiresIn` is 0.
public record FtbFinalToken(
    @SerializedName("token_type") String tokenType,
    @Nullable String scope,
    @SerializedName("expires_in") int expiresIn,
    @SerializedName("refresh_expires_in") int refreshExpiresIn,
    @SerializedName("access_token") String accessToken,
    @SerializedName("id_token") String idToken,
    @SerializedName("refresh_token") String refreshToken
) {}
