package dev.ftb.app.structs.accounts;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

public record DeviceAuthResponse(
    @SerializedName("device_code")
    String deviceCode,
    @SerializedName("user_code")
    String userCode,
    @SerializedName("verification_uri")
    String verificationUri,
    @SerializedName("verification_uri_complete")
    @Nullable String verificationUriComplete,
    @SerializedName("expires_in")
    int expiresIn,
    int interval
) {
    public static final int DEFAULT_INTERVAL = 5;

    public int intervalOrDefault() {
        return interval > 0 ? interval : DEFAULT_INTERVAL;
    }
}
