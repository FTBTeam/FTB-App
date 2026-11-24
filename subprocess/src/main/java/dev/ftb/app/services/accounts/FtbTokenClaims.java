package dev.ftb.app.services.accounts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/// The account details carried in an FTB access token.
public record FtbTokenClaims(
    String id,
    String username,
    @Nullable String email,
    @Nullable String avatarUrl,
    boolean premium
) {
    private static final String PREMIUM_ROLE = "member:ads";

    /// @return `null` if the token can't be read or has no `sub`
    public static @Nullable FtbTokenClaims read(String accessToken) {
        JsonObject claims = payload(accessToken);
        if (claims == null) {
            return null;
        }

        String id = string(claims, "sub");
        if (id == null) {
            return null;
        }

        String email = string(claims, "email");
        String username = string(claims, "preferred_username");
        if (username == null) {
            username = email != null ? email : id;
        }

        return new FtbTokenClaims(id, username, email, string(claims, "picture"), hasPremiumRole(claims));
    }

    private static boolean hasPremiumRole(JsonObject claims) {
        if (!(claims.get("resource_access") instanceof JsonObject access)
            || !(access.get(FtbTokenExchange.CLIENT_ID) instanceof JsonObject client)
            || !client.has("roles")
            || !client.get("roles").isJsonArray()) {
            return false;
        }

        for (JsonElement role : client.getAsJsonArray("roles")) {
            if (role.isJsonPrimitive() && PREMIUM_ROLE.equals(role.getAsString())) {
                return true;
            }
        }

        return false;
    }

    private static @Nullable JsonObject payload(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            var element = JsonParser.parseString(payload);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static @Nullable String string(JsonObject claims, String name) {
        var value = claims.get(name);
        if (value == null || !value.isJsonPrimitive()) {
            return null;
        }

        String text = value.getAsString();
        return text.isBlank() ? null : text;
    }
}
