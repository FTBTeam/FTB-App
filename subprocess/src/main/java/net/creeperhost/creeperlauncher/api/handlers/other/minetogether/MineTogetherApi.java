package net.creeperhost.creeperlauncher.api.handlers.other.minetogether;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.util.Result;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MineTogetherApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(MineTogetherApi.class);

    /**
     * At some point this will stop working and we'll need to try and get a new login
     */
    public static Result<BasicDataAndAccount, String> getCurrentUser(String phpSessionId, boolean sessionId) {
        var client = Constants.httpClient();
        var request = new Request.Builder()
            .url("https://minetogether.io/api/me")
            .header(sessionId ? "App-Sess-ID" : "App-Auth", phpSessionId)
            .header("Accept", "application/json")
            .build();

        try {
            Response execute = client.newCall(request).execute();
            if (execute.code() != 200) {
                return Result.err("Got unexpected response code of %s".formatted(execute.code()));
            }
            
            if (execute.body() == null) {
                return Result.err("Unable to read response data");
            }

            String data = execute.body().string();
            LOGGER.info("Got current user data");
            MineTogetherAccount account = new Gson().fromJson(data, MineTogetherAccount.class);
            var modpackApiKey = account.attributes.getOrDefault("modpackschkey", List.of()).get(0);
            var accounts = account.accounts;
            
            // Try and pull the mcauth account
            String username = "";
            for (MineTogetherAccount.Account provider : accounts) {
                if (Objects.equals(provider.identityProvider, "mcauth")) {
                    username = provider.userName;
                }
            }
            
            S3Credentials s3Credentials = seekS3Credentials(account);
            
            // We should get back a "app-token" header if apiKey is used (data.token)
            return Result.ok(
                new BasicDataAndAccount(new BasicProfileData(username, modpackApiKey, s3Credentials), account)
            );
        } catch (IOException e) {
            LOGGER.error("Failed to get current user", e);
            return Result.err("Failed to get current user");
        }
    }
    
    @Nullable
    private static S3Credentials seekS3Credentials(MineTogetherAccount account) {
        var plan = account.activePlan;
        if (plan == null) {
            return null;
        }
        
        String key = "";
        String secret = "";
        String server = "";
        String bucket = "";
        
        var fields = plan.customFields.customfield;
        for (MineTogetherAccount.Customfield field : fields) {
            switch (field.name) {
                case "S3 Key" -> key = field.value;
                case "S3 Secret" -> secret = field.value;
                case "S3 Server" -> server = field.value;
                case "S3 Bucket" -> bucket = field.value;
            }
        }
        
        if (key.isEmpty() || secret.isEmpty() || server.isEmpty() || bucket.isEmpty()) {
            return null;
        }
        
        return new S3Credentials(key, secret, bucket, server);
    }

    /**
     * Asks Minetogether for a user profile
     */
    public static Result<MineTogetherProfile, String> profile(String username) {
        var client = Constants.httpClient();
        
        var map = new HashMap<String, String>();
        map.put("target", username);
        
        var request = new Request.Builder()         
            .post(RequestBody.create(new Gson().toJson(map), MediaType.parse("application/json")))
            .url("https://api.creeper.host/minetogether/profile");
        
        try {
            Response execute = client.newCall(request.build()).execute();
            if (execute.body() == null) {
                return Result.err("Unable to read response data");
            }
            
            var bodyAsJson = JsonParser.parseString(execute.body().string());
            // This body is a bit of a mess
            var profileData = bodyAsJson.getAsJsonObject().get("profileData");
            if (profileData == null) {
                return Result.err("Unable to find profile data");
            }
            
            // This is a map that contains a key of the users hash... Let's just be lazy and get the first key
            var profileDataAsJson = profileData.getAsJsonObject();
            var firstKey = profileDataAsJson.keySet().iterator().next();
            var actualProfileData = profileDataAsJson.get(firstKey);
            
            // Now parse this data into the MineTogetherProfile
            var profile = new Gson().fromJson(actualProfileData, MineTogetherProfile.class);
            LOGGER.info("Got profile data");
            
            return Result.ok(profile);
        } catch (IOException e) {
            LOGGER.error("Failed to get profile", e);
            return Result.err("Failed to get profile");
        }
    }

    
    public record BasicDataAndAccount(BasicProfileData data, MineTogetherAccount account) {}
}
