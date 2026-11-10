package dev.ftb.app.structs.accounts;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

/// The token response from `login.microsoftonline.com/.../oauth2/v2.0/token` once the device flow completes.
/// Microsoft also sends `ext_expires_in` and, for some scopes, `id_token`. Neither is needed for the Xbox flow.
public record MicrosoftFinalToken(
    @SerializedName("token_type") String tokenType,
    @Nullable String scope,
    @SerializedName("expires_in") int expiresIn,
    @SerializedName("access_token") String accessToken,
    @SerializedName("refresh_token") String refreshToken
) {
    public OAuthTokenHolder toHolder() {
        return new OAuthTokenHolder(accessToken, refreshToken, expiresIn);
    }
}
