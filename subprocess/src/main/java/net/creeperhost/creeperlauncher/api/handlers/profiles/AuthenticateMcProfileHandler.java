package net.creeperhost.creeperlauncher.api.handlers.profiles;

import com.google.gson.Gson;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import okhttp3.*;

import java.io.IOException;
import java.util.UUID;

/**
 * The authentication server has CORS! Ughhh! Looks like we're doing this here now :P
 */
public class AuthenticateMcProfileHandler implements IMessageHandler<AuthenticateMcProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        try {
            Response request = client.newCall(
                    new Request.Builder()
                            .url("https://authserver.mojang.com/authenticate")
                            .post(RequestBody.create(new Gson().toJson(new RequestData(data.username, data.password, UUID.randomUUID().toString())), MediaType.parse("application/json")))
                            .addHeader("Content-Type", "application/json")
                            .addHeader("User-Agent", "FTB App Subprocess/1.0")
                            .addHeader("Accept", "application/json")
                            .build()
            ).execute();

            ResponseBody body = request.body();
            if (body != null) {
                String response = body.string();
                if (response.contains("error")) {
                    Settings.webSocketAPI.sendMessage(new Reply(data, false, response));
                    return;
                }
                Settings.webSocketAPI.sendMessage(new Reply(data, true, response));
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Settings.webSocketAPI.sendMessage(new Reply(data, false, ""));
        }

        Settings.webSocketAPI.sendMessage(new Reply(data, false, ""));
    }

    private static class Reply extends Data {
        public boolean success;
        public String response;

        public Reply(Data data, boolean success, String rawResult) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.success = success;
            this.response = rawResult;
        }
    }

    public static class Data extends BaseData {
        public String username;
        public String password;
    }

    public static class RequestData {
        Agent agent = new Agent("Minecraft", 1);
        public String username;
        public String password;
        public String clientToken;
        public boolean requestUser = true;

        public RequestData(String username, String password, String clientToken) {
            this.username = username;
            this.password = password;
            this.clientToken = clientToken;
        }

        private static class Agent {
            String name;
            int version;

            public Agent(String name, int version) {
                this.name = name;
                this.version = version;
            }
        }
    }

}
