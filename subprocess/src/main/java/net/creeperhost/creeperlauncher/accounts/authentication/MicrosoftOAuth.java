package net.creeperhost.creeperlauncher.accounts.authentication;

import com.google.gson.*;
import net.creeperhost.creeperlauncher.accounts.stores.MSAuthStore;
import net.creeperhost.creeperlauncher.util.MiscUtils;
import okhttp3.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

public class MicrosoftOAuth {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

    private static final Request.Builder JSON_REQUEST = new Request.Builder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");

    public static Pair<JsonObject, MSAuthStore> runFlow(String authToken, String liveRefreshToken, int liveExpiresAt, Consumer<Pair<Boolean, String>> onStage) {
        StepReply authXboxRes = authenticateWithXbox(authToken);

        if (isUnsuccessful(authXboxRes)) {
            onStage.accept(Pair.of(false, "Failed to authenticate with Xbox"));
            return null;
        }

        String xblToken = authXboxRes.data.getAsJsonObject().get("Token").getAsString();
        String userHash = authXboxRes.data.getAsJsonObject().get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString(); // ffs json
        Instant xblIssuedAt = Instant.now();
        if (xblToken.isEmpty()) {
            onStage.accept(Pair.of(false, "Unable to authenticate with Xbox Live..."));
            return null; // Report error here, tokens missing? Why
        }

        // This token expires after 24 hours.
        onStage.accept(Pair.of(true, "Authenticated with Xbox"));

        // Authenticate with XSTS (the dev docs for this are private so fuck knows what it's actually doing)
        StepReply xstsRes = authenticateWithXSTS(xblToken);
        if (isUnsuccessful(xstsRes)) {
            onStage.accept(Pair.of(false, "Failed to authenticate with XSTS"));
            return null;
        }

        // Sometimes this will succeed but fail nicely with a 401
        if (xstsRes.rawResponse.code() == 401) {
            // Get the correct error message, there are more but... again, no docs
            String error = switch (xstsRes.data.getAsJsonObject().get("XErr").getAsString()) {
                case "2148916233" -> "This account does not have an XBox Live account. You have likely not migrated your account.";
                case "2148916235" -> "Your account resides in a region that does not support Xbox Live...";
                case "2148916238" -> "This is an under 18 account. You cannot proceed unless the account is added to a Family by an adult";
                default -> "Unknown XSTS error";
            };

            onStage.accept(Pair.of(false, error));
            return null;
        }

        // Get the XSTS token
        String xstsToken = xstsRes.data.getAsJsonObject().get("Token").getAsString();
        String xstsUserHash = xstsRes.data.getAsJsonObject().get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString(); // ffs json
        Instant xstsIssuedAt = Instant.now();
        if (xstsToken.isEmpty()) {
            onStage.accept(Pair.of(false, "Unable to authenticate with XSTS... (no token found)"));
            return null;
        }

        // We've authenticated with XSTS
        onStage.accept(Pair.of(true, "Authenticated with XSTS"));

        // Login with xbox
        StepReply loginWithXbox = loginWithXbox(xstsToken, userHash);
        if (isUnsuccessful(loginWithXbox)) {
            onStage.accept(Pair.of(false, "Unable to login with xbox live to your Minecraft account"));
            return null;
        }

        // Grab the access token
        String accessToken = loginWithXbox.data.getAsJsonObject().get("access_token").getAsString();
        if (accessToken.isEmpty()) {
            onStage.accept(Pair.of(false, "Unable to login with xbox live to your Minecraft account (no access token found)"));
            return null;
        }

        // We logged in
        onStage.accept(Pair.of(true, "Logged in with Xbox"));
        onStage.accept(Pair.of(true, "Checking ownership of Minecraft"));
        onStage.accept(Pair.of(true, "Fetching Minecraft account"));

        StepReply checkOwnershipRes = checkOwnership(accessToken);
        StepReply profileRes = getProfile(accessToken);

        boolean hasOwnership = false;
        if (isUnsuccessful(checkOwnershipRes)) {
            onStage.accept(Pair.of(false, "Unable to check ownership of Minecraft account"));
        } else {
            // Validate the ownership
            JsonArray items = checkOwnershipRes.data.getAsJsonObject().get("items").getAsJsonArray();
            for (JsonElement item : items) {
                String name = item.getAsJsonObject().get("name").getAsString();
                if (name.equals("product_minecraft") || name.equals("game_minecraft")) {
                    hasOwnership = true;
                }
            }
        }

        if (hasOwnership) {
            onStage.accept(Pair.of(true, "Verified ownership of Minecraft account"));
        }

        // Validate the profile
        if (isUnsuccessful(profileRes)) {
            onStage.accept(Pair.of(false, "Unable to fetch profile from Minecraft account..."));
            LOGGER.error("Unable to fetch profile from Minecraft account... {}", profileRes.data.toString());
            return null;
        }

        // Figure out the type of account
        JsonObject profileData = profileRes.data.getAsJsonObject();
        if (!profileData.has("id")) {
            onStage.accept(Pair.of(false, "Unable to fetch profile from Minecraft account (no id found)"));
            return null;
        }

        return Pair.of(profileData, new MSAuthStore(
                MiscUtils.createUuidFromStringWithoutDashes(profileData.get("id").getAsString()),
                accessToken,
                userHash,
                authToken,
                liveRefreshToken,
                Instant.now().getEpochSecond() + liveExpiresAt
        ));
    }

    /**
     * Login with Xbox
     *
     * @implSpec https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_XBL
     */
    private static StepReply authenticateWithXbox(String authenticationToken) {
        return wrapRequest(
                JSON_REQUEST.url(Endpoints.XBL_AUTHENTICATE.getUrl())
                    .post(createJson(new XblAuthRequest(new XblAuthProperties("RPS", "user.auth.xboxlive.com", String.format("d=%s", authenticationToken)), "http://auth.xboxlive.com", "JWT")))
                    .build(),
                "Successfully authenticated with Xbox",
                "Failed to authenticate with Xbox"
        );
    }

    /**
     * Login with XSTS
     *
     * @implSpec https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_XSTS
     */
    private static StepReply authenticateWithXSTS(String xblToken) {
        return wrapRequest(
                JSON_REQUEST.url(Endpoints.XSTS_AUTHORIZE.getUrl())
                    .post(createJson(new XstsAuthRequest(new XstsAuthProperties("RETAIL", List.of(xblToken)), "rp://api.minecraftservices.com/", "JWT")))
                    .build(),
                "Successfully authenticated with XSTS",
                "Failed to authenticate with XSTS"
        );
    }

    /**
     * Login with Xbox to the Minecraft Services API
     *
     * @implSpec https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_Minecraft
     */
    private static StepReply loginWithXbox(String identityToken, String userHash) {
        return wrapRequest(
                JSON_REQUEST.url(Endpoints.LOGIN_WITH_XBOX.getUrl())
                    .post(createJson(new LoginWithXboxRequest(String.format("XBL3.0 x=%s;%s", userHash, identityToken))))
                    .build(),
                "Successfully logged in with Xbox",
                "Failed to log in with Xbox"
        );
    }

    /**
     * Check the ownership of the game, if the user is using gamepass you can expect this to be empty
     *
     * @implSpec https://wiki.vg/Microsoft_Authentication_Scheme#Checking_Game_Ownership
     */
    private static StepReply checkOwnership(String minecraftToken) {
        return wrapRequest(
                JSON_REQUEST.url(Endpoints.CHECK_STORE.getUrl()).get()
                    .header("Authorization", String.format("Bearer %s", minecraftToken))
                    .build(),
                "Found ownership",
                "Failed to find ownership"
        );
    }

    /**
     * Get the Minecraft Profile.
     *
     * @implNote Looks like this can be empty if the user is on gamepass and have not used the account in a while. To
     *           solve this we'll just prompt the frontend to sign-in again.
     *
     * @implSpec https://wiki.vg/Microsoft_Authentication_Scheme#Get_the_profile
     */
    public static StepReply getProfile(String minecraftToken) {
        return wrapRequest(
                JSON_REQUEST.url(Endpoints.GET_PROFILE.getUrl())
                    .get()
                    .header("Authorization", String.format("Bearer %s", minecraftToken))
                    .build(),
                "Successfully found profile",
                "Failed to find profile"
        );
    }

    /**
     * Safely wrap the request and attempt to form a readable response whilst logging the major issue if one is presented
     */
    private static StepReply wrapRequest(Request request, String successMessage, String errorMessage) {
        try {
            Response execute = CLIENT.newCall(request).execute();
            return new StepReply(true, execute.code(), successMessage, execute.body() != null ? JsonParser.parseString(execute.body().string()) : JsonNull.INSTANCE, execute);
        } catch (Exception e) {
            LOGGER.fatal("Error executing request to {}", request.url(), e);
            return new StepReply(false, -1, errorMessage, JsonNull.INSTANCE, null);
        }
    }

    /**
     * We do this a lot. let's do it in one place.
     */
    private static boolean isUnsuccessful(StepReply response) {
        return response == null || !response.success || response.data == null || response.data.isJsonNull();
    }

    /**
     * Create a JSON object from the given object
     */
    private static RequestBody createJson(Object any) {
        return RequestBody.create(new Gson().toJson(any), MediaType.parse("application/json"));
    }

    /**
     * See the implementation notes for each request to know what these methods go to.
     */
    private enum Endpoints {
        XBL_AUTHENTICATE("https://user.auth.xboxlive.com/user/authenticate"),
        XSTS_AUTHORIZE("https://xsts.auth.xboxlive.com/xsts/authorize"),
        LOGIN_WITH_XBOX("https://api.minecraftservices.com/authentication/login_with_xbox"),
        CHECK_STORE("https://api.minecraftservices.com/entitlements/mcstore"),
        GET_PROFILE("https://api.minecraftservices.com/minecraft/profile");

        private final String url;

        Endpoints(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    /**
     * Repsonses & requests.
     */
    public record StepReply(
            boolean success,
            int status,
            String message,
            JsonElement data,
            @Nullable Response rawResponse
    ) {}

    private record XblAuthRequest(
            XblAuthProperties Properties,
            String RelyingParty,
            String TokenType
    ) {}

    private record XblAuthProperties(
            String AuthMethod,
            String SiteName,
            String RpsTicket
    ) {}

    private record XstsAuthRequest(
            XstsAuthProperties Properties,
            String RelyingParty,
            String TokenType
    ) {}

    private record XstsAuthProperties(
            String SandboxId,
            List<String> UserTokens
    ) {}

    private record LoginWithXboxRequest(
            String identityToken
    ) {}
}
