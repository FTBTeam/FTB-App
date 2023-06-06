package net.creeperhost.creeperlauncher.accounts.authentication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.covers1624.quack.gson.JsonUtils;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.accounts.stores.MSAuthStore;
import net.creeperhost.creeperlauncher.util.MiscUtils;
import net.creeperhost.creeperlauncher.util.Result;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static net.creeperhost.creeperlauncher.accounts.authentication.ApiRecords.*;

public class MicrosoftOAuth {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Consumer<DanceStep> onUpdate;
    private final DanceContext context;
    
    private MicrosoftOAuth(Consumer<DanceStep> onUpdate, DanceContext context) {
        this.onUpdate = onUpdate;
        this.context = context;
    }
    
    public static MicrosoftOAuth create(DanceContext context, Consumer<DanceStep> onUpdate) {
        return new MicrosoftOAuth(onUpdate, context);
    }

    public Result<DanceResult, DanceCodedError> runOAuthDance() {
        emit("Beginning dance", Step.START_DANCE.createSuccessStep());

        // Login with Xbox live service
        emit("Logging into XBox Live", Step.AUTH_XBOX.createWorkingStep());
        var xblAuthResult = request(new Request.Builder()
                .url(Constants.MS_OAUTH_XBL_AUTHENTICATE)
                .post(jsonBody(new Requests.XblAuth(new Requests.XblAuthProperties("RPS", "user.auth.xboxlive.com", String.format("d=%s", context.authToken)), "http://auth.xboxlive.com", "JWT"))),
            Responses.XblAuth.class
        );

        if (xblAuthResult.isErr()) {
            return emitAndError("Failed to authenticate with XBL", "ftb-auth-000001", Step.AUTH_XBOX.createErrorStep(), xblAuthResult);
        }

        emit("Logged into XBox live", Step.AUTH_XBOX.createSuccessStep());
        emit("Logging into XSTS service", Step.AUTH_XSTS.createWorkingStep());

        // Log into XSTS service 
        var xblAuth = xblAuthResult.unwrap();

        // Login with XSTS service using the token from the xbl auth
        var xstsResult = request(new Request.Builder()
                .url(Constants.MS_OAUTH_XSTS_AUTHORIZE)
                .post(jsonBody(new Requests.XstsAuth(new Requests.XstsAuthProperties("RETAIL", List.of(xblAuth.Token)), "rp://api.minecraftservices.com/", "JWT"))),
            Responses.XblAuth.class
        );

        if (xstsResult.isErr()) {
            String errorCode = "ftb-auth-000002";
            
            // Attempt to figure out what the cause might have been
            RequestError requestError = xstsResult.unwrapErr();
            JsonElement jsonError = requestError.json();
            if (jsonError != null && jsonError.isJsonObject() && jsonError.getAsJsonObject().has("XErr")) {
                errorCode = decipherXErr(jsonError.getAsJsonObject().get("XErr").getAsString());
            }

            return emitAndError("Failed to authenticate with XSTS system", errorCode, Step.AUTH_XSTS.createErrorStep(), xstsResult);
        }

        var xstsAuth = xstsResult.unwrap();

        // Login with Xbox to the Minecraft services
        String userHash = xblAuth.DisplayClaims.xui.get(0).uhs;
        emit("Logged in successfully to XSTS", Step.AUTH_XSTS.createSuccessStep());
        emit("Logging into Minecraft via Xbox", Step.LOGIN_XBOX.createWorkingStep());
        var loginResult = request(new Request.Builder()
                .url(Constants.MS_OAUTH_LAUNCHER_LOGIN)
                .post(jsonBody(new Requests.LoginWithXbox(String.format("XBL3.0 x=%s;%s", userHash, xstsAuth.Token), "PC_LAUNCHER"))),
            Responses.LoginWithXbox.class
        );

        if (loginResult.isErr()) {
            return emitAndError("Failed to login with Xbox", "ftb-auth-000003", Step.LOGIN_XBOX.createErrorStep(), loginResult);
        }

        emit("Logged into Minecraft via Xbox", Step.LOGIN_XBOX.createSuccessStep());
        
        emit("Getting profile", Step.GET_PROFILE.createWorkingStep());
        emit("Checking Entitlements", Step.CHECK_ENTITLEMENTS.createWorkingStep());

        // The minecraft process is super stupid at this point. You can have about 12 different valid states but only 
        // one of them is valid enough for us to pull the UUID from the players profile.
        // 
        // - First we need to see if both checks isErr, this is likely due to network issues.
        // - Next we'll check the ownership claims for a few different flags. See if they own MC, bedrock and gamepass
        // - Then we'll check if the profile was valid, if it's not, we can't progress but with the above we can give 
        //   the user some valuable feedback on why their account couldn't be validated
        // - Finally if the claim or the profile are valid we can continue. Sometimes the claim will be empty but their 
        //   profile will not be... All we actually need is the users profile UUID at this state as we already have the
        //   accounts access token, and we only require the uuid and access token.

        var loginData = loginResult.unwrap();

        // The flowing two requests don't depend on each other and per the above comment, can change the outcome
        var ownershipResult = request(new Request.Builder()
                .url(Constants.MS_OAUTH_CHECK_STORE).get()
                .header("Authorization", String.format("Bearer %s", loginData.access_token)),
            Responses.Entitlements.class
        );

        var profile = fetchMcProfile(loginData.access_token);
        var ownershipClaims = getOwnershipClaims(ownershipResult.unwrapOrElse(() -> null));

        // First, ignore all ownership claims, we don't care
        if (profile.isOk()) {
            emit("Successfully found Minecraft Profile", Step.GET_PROFILE.createSuccessStep());
            emit("Entitlements checked (Lazy)", Step.CHECK_ENTITLEMENTS.createSuccessStep());
            
            // We're good, lets return
            MinecraftProfileData profileData = profile.unwrap();
            return Result.ok(new DanceResult(
                profileData,
                // This all feels like it shouldn't be here...
                new MSAuthStore(
                    MiscUtils.createUuidFromStringWithoutDashes(profileData.id()),
                    loginData.access_token,
                    userHash,
                    context.authToken(),
                    context.refreshToken(),
                    Instant.now().getEpochSecond() + context.expiresAt
                )
            ));
        }

        emit("Failed to find Minecraft Profile", Step.GET_PROFILE.createErrorStep());
        
        // Anything below here is purely to provide valuable information to the user
        if (ownershipClaims == null) {
            return emitAndError("Failed to check Entitlements", "ftb-auth-000004", Step.CHECK_ENTITLEMENTS.createErrorStep(), ownershipResult);
        }

        // Now it's time to provide useful responses to the user
        // should launch the vanilla launcher at least once
        if (ownershipClaims.ownsGamepass()) {
            return emitAndError("Entitlements show that the user owns gamepass", "ftb-auth-000005", Step.CHECK_ENTITLEMENTS.createErrorStep(), false);
        }

        if (ownershipClaims.ownsMinecraft()) {
            // The user owns minecraft... but does not have a profile. This means they need to run the vanilla launcher at least once
            return emitAndError("Entitlements show that the user owns Minecraft", "ftb-auth-000005", Step.CHECK_ENTITLEMENTS.createErrorStep(), false);
        }

        // You just don't have minecraft, wrong account like normal, Request was successful though!
        return emitAndError("Entitlements show that the does not own minecraft", "ftb-auth-000006", Step.CHECK_ENTITLEMENTS.createErrorStep(), false);
    }

    /**
     * Attempt to figure out why the user might not be allowed to log in with xbox
     */
    private static String decipherXErr(String xerr) {
        return switch (xerr) {
            case "2148916233" -> "ftb-auth-000012"; //"This account does not have an XBox Live account. You have likely not migrated your account.";
            case "2148916235" -> "ftb-auth-000013"; //"Your account resides in a region that does not support Xbox Live...";
            case "2148916236", "2148916237" -> "ftb-auth-000014"; //"The account needs adult verification on Xbox page.";
            case "2148916238" -> "ftb-auth-000015"; //"This is an under 18 account. You cannot proceed unless the account is added to a Family by an adult";
            default -> "ftb-auth-000016";
        };
    }

    public static Result<MinecraftProfileData, RequestError> fetchMcProfile(String accessToken) {
        return request(new Request.Builder().get()
                .url(Constants.MC_GET_PROFILE)
                .header("Authorization", String.format("Bearer %s", accessToken)),
            MinecraftProfileData.class
        );
    }
    
    public static Result<Responses.Migration, RequestError> checkMigrationStatus(String accessToken) {
         return request(new Request.Builder().get()
                 .url(Constants.MC_CHECK_MIGRATION)
                 .header("Authorization", String.format("Bearer %s", accessToken)),
             Responses.Migration.class
         );
    }
    
    @Nullable
    private OwnershipClaims getOwnershipClaims(@Nullable Responses.Entitlements entitlements) {
        if (entitlements == null) {
            return null;
        }

        boolean ownsMinecraft = false;
        boolean ownsGamepass = false;
        boolean ownsBedrock = false;

        for (Responses.Entitlements.Entitlement item : entitlements.items) {
            if (item.name.equals("product_minecraft") || item.name.equals("game_minecraft")) {
                ownsMinecraft = true;
            }
            if (item.name.equals("product_game_pass_pc")) {
                ownsGamepass = true;
            }
            if (item.name.equals("game_minecraft_bedrock") || item.name.equals("product_minecraft_bedrock")) {
                ownsBedrock = true;
            }
        }

        return new OwnershipClaims(ownsMinecraft, ownsBedrock, ownsGamepass);
    }


    private static <TResult> Result<TResult, RequestError> request(Request.Builder request, Class<TResult> responseClass) {
        OkHttpClient client = Constants.httpClient();
        Request requestInput = request
            .header("Content-Type", "application/json")
            .header("User-Agent", Constants.USER_AGENT)
            .header("Accept", "application/json")
            .build();

        try (Response response = client.newCall(requestInput).execute()) {
            int status = response.code();
            ResponseBody body = response.body();
            var stringBody = body == null ? "" : body.string();

            if (status < 200 || status >= 300) {
                // Handle >500 differently
                if (status >= 500) {
                    LOGGER.info("Server for {} is currently unavailable", requestInput.url().toString());
                    return Result.err(RequestError.create(response, stringBody, null));
                } else {
                    LOGGER.info("Request to [{}] isErr with a status code of [{}]", requestInput.url().toString(), status);
                }

                if (!stringBody.isEmpty() && Objects.equals(response.header("content-type"), "application/json")) {
                    try {
                        return Result.err(RequestError.create(response, stringBody, JsonUtils.parseRaw(stringBody)));
                    } catch (Exception ignored) {
                    }
                }

                return Result.err(RequestError.create(response, stringBody, null));
            }

            // All our requests expect a body to be returned so it is safe to return fail here.
            if (body == null) {
                LOGGER.info("Request to [{}] isErr as the body was empty", requestInput.url().toString());
                return Result.err(RequestError.create(response, stringBody, null));
            }

            // Try and parse 
            TResult jsonResponse = JsonUtils.parse(new Gson(), stringBody, responseClass);
            return Result.ok(jsonResponse);
        } catch (IOException | JsonParseException e) {
            if (e instanceof JsonParseException) {
                LOGGER.error("Request to [{}] due to the response body being invalid", requestInput.url().toString(), e);
            }
      
            return Result.err(RequestError.create(null, "", null));
        }
    }

    enum Step {
        START_DANCE,
        AUTH_XBOX,
        AUTH_XSTS,
        LOGIN_XBOX,
        GET_PROFILE,
        CHECK_ENTITLEMENTS;

        DanceStep createSuccessStep() {
            return new DanceStep(this, true, false, false);
        }

        DanceStep createErrorStep() {
            return new DanceStep(this, false, true, false);
        }
        
        DanceStep createWorkingStep() {
            return new DanceStep(this, false, false, true);
        }
    }

    record DanceStep(
        Step step,
        boolean successful,
        boolean error,
        boolean working
    ) {}
    
    private void emit(String message, DanceStep step) {
        LOGGER.info(message);
        this.onUpdate.accept(step);
    }

    private <T> Result<T, DanceCodedError> emitAndError(String message, String code, DanceStep step, Result<?, RequestError> resultError) {
        return this.emitAndError(message, code, step, resultError.unwrapErr().status() == -1);
    }

    private <T> Result<T, DanceCodedError> emitAndError(String message, String code, DanceStep step, boolean networkError) {
        this.emit(message, step);
        return Result.err(new DanceCodedError(networkError, code));
    }

    private static RequestBody jsonBody(Object any) {
        return RequestBody.create(new Gson().toJson(any), MediaType.parse("application/json"));
    }

    record DanceContext(
        String authToken,
        String refreshToken,
        int expiresAt
    ) {
    }

    public record DanceResult(
        MinecraftProfileData profile,
        MSAuthStore store
    ) {}
    
    public record DanceCodedError(
        boolean networkError,
        String code
    ) {}

    public record RequestError(
        String body,
        int status,
        @Nullable JsonElement json,
        @Nullable Response response
    ) {
        static RequestError create(Response response, String body, @Nullable JsonElement json) {
            if (response == null) {
                return new RequestError(body, -1, json, null);
            }

            return new RequestError(body, response.code(), json, response);
        }
    }

    private record OwnershipClaims(
        boolean ownsMinecraft,
        boolean ownsBedrock,
        boolean ownsGamepass
    ) {
    }
}
