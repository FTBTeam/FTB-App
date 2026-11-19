package dev.ftb.app.structs.accounts;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

/// The token response from the FTB identity server (Keycloak) once the device flow completes.
///
/// The access token is what the modpack API accepts, and what account details are read from. The id token isn't
/// used or stored, so may be missing. With `offline_access` the refresh token
/// doesn't expire and `refreshExpiresIn` is 0.
public record FtbFinalToken(
    @SerializedName("token_type") String tokenType,
    @Nullable String scope,
    @SerializedName("expires_in") int expiresIn,
    @SerializedName("refresh_expires_in") int refreshExpiresIn,
    @SerializedName("access_token") String accessToken,
    @SerializedName("id_token") @Nullable String idToken,
    @SerializedName("refresh_token") String refreshToken
) {}
