package net.creeperhost.creeperlauncher.accounts.authentication;

import com.google.gson.*;
import net.creeperhost.creeperlauncher.accounts.data.ErrorWithCode;
import net.creeperhost.creeperlauncher.accounts.data.StepReply;
import net.creeperhost.creeperlauncher.accounts.stores.MSAuthStore;
import net.creeperhost.creeperlauncher.util.DataResult;
import net.creeperhost.creeperlauncher.util.MiscUtils;
import okhttp3.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.*;

public class MicrosoftOAuth {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

    private static final Request.Builder JSON_REQUEST = new Request.Builder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");

    @Nonnull
    public static DataResult<Pair<JsonObject, MSAuthStore>, ErrorWithCode> runFlow(String authToken, String liveRefreshToken, int liveExpiresAt) {
        LOGGER.info("Starting Microsoft Authentication flow...");
        StepReply authXboxRes = authenticateWithXbox(authToken);

        if (isUnsuccessful(authXboxRes)) {
            return DataResult.error(new ErrorWithCode("Failed to authenticate with Xbox", "xbx_auth_001"));
        }

        String xblToken = authXboxRes.data().getAsJsonObject().get("Token").getAsString();
        String userHash = authXboxRes.data().getAsJsonObject().get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString(); // ffs json
        Instant xblIssuedAt = Instant.now();
        if (xblToken.isEmpty()) {
            return DataResult.error(new ErrorWithCode("Unable to authenticate with Xbox Live...", "xbx_auth_002"));
        }

        // This token expires after 24 hours.
        LOGGER.info("Authenticated with Xbox ✅");

        // Authenticate with XSTS (the dev docs for this are private so fuck knows what it's actually doing)
        StepReply xstsRes = authenticateWithXSTS(xblToken);
        if (isUnsuccessful(xstsRes)) {
            return DataResult.error(new ErrorWithCode("Failed to authenticate with XSTS", "xbx_auth_003"));
        }

        // Sometimes this will succeed but fail nicely with a 401
        if (xstsRes.rawResponse().code() == 401) {
            // Get the correct error message, there are more but... again, no docs
            String xErr = xstsRes.data().getAsJsonObject().get("XErr").getAsString();
            String error = switch (xErr) {
                case "2148916233" -> "This account does not have an XBox Live account. You have likely not migrated your account.";
                case "2148916235" -> "Your account resides in a region that does not support Xbox Live...";
                case "2148916236", "2148916237" -> "The account needs adult verification on Xbox page.";
                case "2148916238" -> "This is an under 18 account. You cannot proceed unless the account is added to a Family by an adult";
                default -> "Unknown XSTS error";
            };

            return DataResult.error(new ErrorWithCode(error, "xbx_auth_004_" + xErr));
        }

        // Get the XSTS token
        String xstsToken = xstsRes.data().getAsJsonObject().get("Token").getAsString();
        String xstsUserHash = xstsRes.data().getAsJsonObject().get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString(); // ffs json
        Instant xstsIssuedAt = Instant.now();
        if (xstsToken.isEmpty()) {
            return DataResult.error(new ErrorWithCode("Unable to authenticate with XSTS... (no token found)", "xbx_auth_005"));
        }

        // We've authenticated with XSTS
        LOGGER.info("Authenticated with XSTS ✅");

        // Login with xbox
        StepReply loginWithXbox = loginWithXbox(xstsToken, userHash);
        if (isUnsuccessful(loginWithXbox)) {
            return DataResult.error(new ErrorWithCode("Unable to login with xbox live to your Minecraft account", "xbx_auth_006"));
        }

        // Grab the access token
        String accessToken = loginWithXbox.data().getAsJsonObject().get("access_token").getAsString();
        if (accessToken.isEmpty()) {
            return DataResult.error(new ErrorWithCode("Unable to login with xbox live to your Minecraft account (no access token found)", "xbx_auth_006"));
        }

        // We logged in
        LOGGER.info("Logged in with Xbox");
        LOGGER.info("Checking ownership of Minecraft");
        StepReply checkOwnershipRes = checkOwnership(accessToken);

        LOGGER.info("Fetching Minecraft account");
        StepReply profileRes = getProfile(accessToken);

        boolean hasOwnership = false;
        if (isUnsuccessful(checkOwnershipRes)) {
            LOGGER.warn("Unable to check ownership of Minecraft account");
        } else {
            // Validate the ownership
            JsonArray items = checkOwnershipRes.data().getAsJsonObject().get("items").getAsJsonArray();
            for (JsonElement item : items) {
                String name = item.getAsJsonObject().get("name").getAsString();
                if (name.equals("product_minecraft") || name.equals("game_minecraft")) {
                    hasOwnership = true;
                }
            }
        }

        LOGGER.info("Verified ownership of Minecraft account... result: {}", hasOwnership ? "Does not own" : "Owns");

        // Validate the profile
        if (isUnsuccessful(profileRes)) {
            return DataResult.error(new ErrorWithCode("Unable to fetch profile from Minecraft account...", "xbx_auth_007"));
        }

        // Figure out the type of account
        JsonObject profileData = profileRes.data().getAsJsonObject();
        if (!profileData.has("id")) {
            return DataResult.error(new ErrorWithCode("Unable to fetch profile from Minecraft account (no id found)", "xbx_auth_008"));
        }

        return DataResult.data(Pair.of(profileData, new MSAuthStore(
                MiscUtils.createUuidFromStringWithoutDashes(profileData.get("id").getAsString()),
                accessToken,
                userHash,
                authToken,
                liveRefreshToken,
                Instant.now().getEpochSecond() + liveExpiresAt
        )));
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
                JSON_REQUEST.url(Endpoints.LAUNCHER_LOGIN.getUrl())
                    .post(createJson(new LoginWithXboxRequest(String.format("XBL3.0 x=%s;%s", userHash, identityToken), "PC_LAUNCHER")))
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

    public static StepReply checkMigrationStatus(String minecraftToken) {
        return wrapRequest(
                JSON_REQUEST.url(Endpoints.CHECK_MIGRATION.getUrl())
                        .get()
                        .header("Authorization", String.format("Bearer %s", minecraftToken))
                        .build(),
                "Migration status checked",
                "Failed to check migration status"
        );
    }

    /**
     * Safely wrap the request and attempt to form a readable response whilst logging the major issue if one is presented
     */
    private static StepReply wrapRequest(Request request, String successMessage, String errorMessage) {
        try {
            LOGGER.info("Making authentication request to {}", request.url());
            Response execute = CLIENT.newCall(request).execute();
            ResponseBody body = execute.body();
            if (body != null) {
                try {
                    JsonElement jsonElement = JsonParser.parseString(body.string());
                    LOGGER.info("{}|{} responded with: {}", execute.code(), request.url(), jsonElement.toString().replaceAll("\"ey[a-zA-Z0-9._-]+", "****"));
                    return new StepReply(true, execute.code(), successMessage, jsonElement, execute);
                } catch (JsonParseException exception) {
                    LOGGER.fatal("Unable to parse json response from {} with error of {}", request.url(), exception);
                }
            }

            LOGGER.fatal("Failed to read and handle response from {}", request.url());
            return new StepReply(false, -1, errorMessage, JsonNull.INSTANCE, null);
        } catch (Exception e) {
            LOGGER.fatal("Error executing request to {}", request.url(), e);
            return new StepReply(false, -1, errorMessage, JsonNull.INSTANCE, null);
        }
    }

    /**
     * We do this a lot. let's do it in one place.
     */
    private static boolean isUnsuccessful(StepReply response) {
        return response == null || !response.success() || response.data() == null || response.data().isJsonNull();
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
        LAUNCHER_LOGIN("https://api.minecraftservices.com/launcher/login"),
        CHECK_STORE("https://api.minecraftservices.com/entitlements/mcstore"),
        GET_PROFILE("https://api.minecraftservices.com/minecraft/profile"),
        CHECK_MIGRATION("https://api.minecraftservices.com/rollout/v1/msamigration");

        private final String url;

        Endpoints(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

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
            String xtoken,
            String platform
    ) {}
}
