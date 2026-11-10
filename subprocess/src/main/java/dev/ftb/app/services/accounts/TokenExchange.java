package dev.ftb.app.services.accounts;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.ftb.app.services.AppHttp;
import dev.ftb.app.structs.accounts.DeviceAuthResponse;
import dev.ftb.app.util.Result;
import dev.ftb.app.util.StringUtils;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// An OAuth 2.0 device authorization grant against a single identity provider.
public abstract class TokenExchange<T> {
    protected static final String DEVICE_CODE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";

    private final Logger logger;
    private final Gson gson;
    private final AppHttp appHttp;

    private final String providerName;
    private final Class<T> tokenType;

    protected final String deviceCodeUrl;
    protected final String tokenUrl;
    protected final String clientId;
    protected final String scope;

    protected TokenExchange(
        Logger logger,
        Gson gson,
        AppHttp http,
        String providerName,
        Class<T> tokenType,
        String deviceCodeUrl,
        String tokenUrl,
        String clientId,
        String scope
    ) {
        this.logger = logger;
        this.gson = gson;
        this.appHttp = http;
        this.providerName = providerName;
        this.tokenType = tokenType;
        this.deviceCodeUrl = deviceCodeUrl;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.scope = scope;
    }

    /// Requests a new device code and user code from the provider.
    public Result<DeviceAuthResponse, String> initiate() {
        var request = new Request.Builder()
            .url(deviceCodeUrl)
            .header("Accept", "application/json")
            .post(initiateBody())
            .build();

        try (Response response = appHttp.httpClient().newCall(request).execute()) {
            JsonObject json = readJson(response);

            if (!response.isSuccessful()) {
                var error = toProviderError(response.code(), json);
                logger.error("Failed to request a {} device code: {}", providerName, error);
                return Result.err("Failed to get a login code from " + providerName + " (" + describe(error) + "). Retrying may fix this.");
            }

            if (json == null) {
                return Result.err(providerName + " returned an empty or unreadable response. Retrying may fix this.");
            }

            var parsed = gson.fromJson(json, DeviceAuthResponse.class);
            if (StringUtils.isBlank(parsed.deviceCode()) || StringUtils.isBlank(parsed.userCode()) || StringUtils.isBlank(parsed.verificationUri()) || parsed.expiresIn() <= 0) {
                logger.error("{} device code response was missing fields: {}", providerName, json.keySet());
                return Result.err(providerName + " returned an incomplete login code. Retrying may fix this.");
            }

            return Result.ok(parsed);
        } catch (IOException | JsonParseException e) {
            logger.error("Failed to request a {} device code", providerName, e);
            return Result.err("Couldn't reach " + providerName + " to get a login code. Check your connection and retry.");
        }
    }

    /// Asks the provider whether the user has finished signing in for `deviceCode`. Makes exactly one request.
    public PollState<T> poll(String deviceCode) {
        var request = new Request.Builder()
            .url(tokenUrl)
            .header("Accept", "application/json")
            .post(pollBody(deviceCode))
            .build();

        try (Response response = appHttp.httpClient().newCall(request).execute()) {
            JsonObject json = readJson(response);

            if (response.isSuccessful()) {
                if (json == null) {
                    return new PollState.Pending<>(false, true, new ProviderError(response.code(), null, "Empty token response", null));
                }

                T token = gson.fromJson(json, tokenType);
                if (!isTokenValid(token)) {
                    logger.error("{} token response was missing fields: {}", providerName, json.keySet());
                    return new PollState.Failed<>(providerName + " didn't return a complete token. Please try again.", null);
                }

                return new PollState.Done<>(token);
            }

            var error = toProviderError(response.code(), json);
            
            // Try and figure out what the error is. 
            // We also support some special types from Microsoft.
            return switch (error.error() == null ? "" : error.error()) {
                case "authorization_pending" -> new PollState.Pending<>(false, false, error);
                case "slow_down" -> new PollState.Pending<>(true, false, error);
                case "expired_token", "code_expired" -> new PollState.Failed<>("The login code has expired. Please try again.", error);
                case "authorization_declined", "access_denied" -> new PollState.Failed<>("The login request was declined. Try again, or close this window.", error);
                // Special case for Microsoft
                case "bad_verification_code", "invalid_grant" -> new PollState.Failed<>(providerName + " didn't recognise the login code. Please try again.", error);
                case "invalid_scope", "invalid_client", "unauthorized_client", "unsupported_grant_type" -> {
                    logger.error("{} rejected the app's client configuration: {}", providerName, error);
                    yield new PollState.Failed<>("The FTB App's " + providerName + " login isn't set up correctly. Please report this at https://github.com/FTBTeam/FTB-App/issues", error);
                }
                default -> {
                    // Rate limiting and server errors are worth riding out, the device code is still valid
                    if (response.code() == 429 || response.code() >= 500) {
                        yield new PollState.Pending<>(response.code() == 429, true, error);
                    }

                    logger.error("Unexpected response while polling for a {} token: {}", providerName, error);
                    yield new PollState.Failed<>(providerName + " returned an unexpected error (" + describe(error) + "). Please try again.", error);
                }
            };
        } catch (IOException | JsonParseException e) {
            // One dropped request shouldn't end the login, keep polling until the code expires
            logger.warn("Failed to poll for a {} token, will retry: {}", providerName, e.toString());
            return new PollState.Pending<>(false, true, new ProviderError(0, null, e.toString(), null));
        }
    }

    /// The form sent to the device code endpoint. Override if a provider needs more than the RFC's fields.
    protected FormBody initiateBody() {
        return new FormBody.Builder()
            .add("client_id", clientId)
            .add("scope", scope)
            .build();
    }

    /// The form sent to the token endpoint on each poll. Override if a provider needs more than the RFC's fields.
    protected FormBody pollBody(String deviceCode) {
        return new FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", DEVICE_CODE_GRANT)
            .add("device_code", deviceCode)
            .build();
    }

    /// Whether a token parsed from a successful poll has everything the rest of the login needs.
    protected abstract boolean isTokenValid(T token);
    
    // TODO: Should we move this to a shared utility?
    private @Nullable JsonObject readJson(Response response) throws IOException {
        var body = response.body();
        if (body == null) {
            return null;
        }

        var text = body.string();
        if (text.isBlank()) {
            return null;
        }

        try {
            JsonElement element = gson.fromJson(text, JsonElement.class);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (JsonParseException e) {
            return null;
        }
    }

    /// Converts a provider's error response into a structured object. `json` may be null if the request failed to reach the server.
    private static ProviderError toProviderError(int status, @Nullable JsonObject json) {
        if (json == null) {
            return new ProviderError(status, null, null, null);
        }

        List<Long> codes = null;
        if (json.has("error_codes") && json.get("error_codes").isJsonArray()) {
            codes = new ArrayList<>();
            for (var code : json.getAsJsonArray("error_codes")) {
                if (code.isJsonPrimitive() && code.getAsJsonPrimitive().isNumber()) {
                    codes.add(code.getAsLong());
                }
            }
        }

        return new ProviderError(
            status,
            stringOrNull(json, "error"),
            stringOrNull(json, "error_description"),
            codes
        );
    }

    private static String describe(ProviderError error) {
        return error.error() != null ? error.error() + ", HTTP " + error.httpStatus() : "HTTP " + error.httpStatus();
    }

    private static @Nullable String stringOrNull(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : null;
    }
    
    public sealed interface PollState<T> {
        record Pending<T>(boolean slowDown, boolean retry, @Nullable ProviderError detail) implements PollState<T> {}
        record Done<T>(T token) implements PollState<T> {}
        record Failed<T>(String message, @Nullable ProviderError detail) implements PollState<T> {}
    }
    
    public record ProviderError(int httpStatus, @Nullable String error, @Nullable String description, @Nullable List<Long> codes) {}
}
