package net.creeperhost.creeperlauncher.accounts.authentication;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.accounts.stores.YggdrasilAuthStore;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.UUID;

/**
 * *Slaps car*
 * This baby fits so much useless time into it.
 */
public class MojangAuthenticator implements AuthenticatorValidator<YggdrasilAuthStore, MojangAuthenticator.LoginData, String> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Agent AGENT = new Agent("Minecraft", 1);

    private static final Request.Builder BASIC_REQUEST = new Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "FTB App Subprocess/1.0")
            .addHeader("Accept", "application/json");

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

    @Override
    public boolean isValid(AccountProfile profile) {
        if (profile.isMicrosoft || profile.mcAuth == null) {
            LOGGER.warn("Microsoft accounts cannot be validated");
            return false;
        }

        try {
            Response request = CLIENT.newCall(
                    BASIC_REQUEST
                            .url("https://authserver.mojang.com/validate")
                            .post(RequestBody.create(new Gson().toJson(new ValidateRequestData(
                                    profile.mcAuth.accessToken,
                                    profile.mcAuth.clientToken
                            )), MediaType.parse("application/json")))
                            .build()
            ).execute();

            LOGGER.info("Received response from Mojang's authentication server with status code " + request.code());
            if (request.code() == 204) {
                return true;
            }

            ResponseBody body = request.body();
            LOGGER.warn("Mojang returned an error: " + (body != null ? body.string() : "Unknown error"));
        } catch (IOException e) {
            LOGGER.fatal("Failed to check for validity with Mojang", e);
        }

        return false;
    }

    /**
     * The final string isn't needed so we just ignore it
     */
    @Nullable
    @Override
    public Reply<YggdrasilAuthStore> refresh(AccountProfile profile, String ignored) {
        if (profile.isMicrosoft || profile.mcAuth == null) {
            LOGGER.warn("Microsoft accounts cannot be refreshed");
            return new Reply<>(null, false, "Microsoft accounts cannot be refreshed");
        }

        try {
            Response request = CLIENT.newCall(
                    BASIC_REQUEST
                            .url("https://authserver.mojang.com/refresh")
                            .post(RequestBody.create(new Gson().toJson(new RefreshRequestData(
                                    profile.mcAuth.accessToken,
                                    profile.mcAuth.clientToken,
                                    true
                            )), MediaType.parse("application/json")))
                            .build()
            ).execute();

            LOGGER.info("Received response from Mojang's authentication server with status code " + request.code());
            if (request.code() == 403) {
                return new Reply<>(null, false, "Invalid refresh token");
            }

            ResponseBody body = request.body();
            if (body != null) {
                String response = body.string();
                JsonObject resData = JsonParser.parseString(response).getAsJsonObject();
                if (resData.has("accessToken")) {
                    return new Reply<>(
                        new YggdrasilAuthStore(
                                resData.get("accessToken").getAsString(),
                                resData.get("clientToken").getAsString()
                        ), true, "Success"
                    );
                }
            }

            LOGGER.warn("Mojang returned an error");
            return new Reply<>(null, false, body != null ? body.string() : "Unknown error");
        } catch (IOException e) {
            LOGGER.fatal("Failed to check for validity with Mojang", e);
        }

        return null;
    }

    @Nullable
    @Override
    public Reply<YggdrasilAuthStore> authenticate(LoginData req) {
        UUID randomId = UUID.randomUUID();

        try {
            Response request = CLIENT.newCall(
                    BASIC_REQUEST
                            .url("https://authserver.mojang.com/authenticate")
                            .post(RequestBody.create(new Gson().toJson(new RequestData(AGENT, req.username, req.password, randomId.toString(), true)), MediaType.parse("application/json")))
                            .build()
            ).execute();

            ResponseBody body = request.body();
            LOGGER.info("Received response from Mojang's authentication server with status code " + request.code());
            if (body != null) {
                String response = body.string();
                JsonObject resData = JsonParser.parseString(response).getAsJsonObject();
                if (resData.has("accessToken")) {
                    return new Reply<>(
                        new YggdrasilAuthStore(
                                resData.get("accessToken").getAsString(),
                                resData.get("clientToken").getAsString()
                        ), true, "Success"
                    );
                } else {
                    LOGGER.warn("Mojang returned an error: " + response);
                    return new Reply<>(null, false, response);
                }
            }
        } catch (IOException e) {
            LOGGER.fatal("Failed to authenticate with Mojang", e);
        }

        return null;
    }

    public record LoginData(
            String username,
            String password
    ) {
    }

    record ValidateRequestData(
            String accessToken,
            String clientToken
    ) {
    }

    record RefreshRequestData(
            String accessToken,
            String clientToken,
            boolean requestUser
    ) {
    }

    record RequestData(
            Agent agent,
            String username,
            String password,
            String clientToken,
            boolean requestUser
    ) {
    }

    record Agent(
            String name,
            int version
    ) {
    }
}
