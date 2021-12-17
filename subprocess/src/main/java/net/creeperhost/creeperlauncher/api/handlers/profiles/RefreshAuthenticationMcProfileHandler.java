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
public class RefreshAuthenticationMcProfileHandler implements IMessageHandler<RefreshAuthenticationMcProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        try {
            Response request = client.newCall(
                    new Request.Builder()
                            .url("https://authserver.mojang.com/refresh")
                            .post(RequestBody.create(new Gson().toJson(new RequestData(data.accessToken, data.clientToken, new RequestData.SelectedAccount(data.userUuid.toString().replaceAll("'", ""), data.userName))), MediaType.parse("application/json")))
                            .addHeader("Content-Type", "application/json")
                            .addHeader("User-Agent", "FTB App Subprocess/1.0")
                            .addHeader("Accept", "application/json")
                            .build()
            ).execute();

            ResponseBody body = request.body();
            if (body != null) {
                Settings.webSocketAPI.sendMessage(new Reply(data, true, body.string()));
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
        public String accessToken;
        public String clientToken;
        public UUID userUuid;
        public String userName;
    }

    public static class RequestData {
        public String accessToken;
        public String clientToken;
        public boolean requestUser = true;
        public SelectedAccount selectedProfile;
        
        public RequestData(String accessToken, String clientToken, SelectedAccount selectedProfile) {
            this.accessToken = accessToken;
            this.clientToken = clientToken;
            this.selectedProfile = selectedProfile;
        }
        
        private static class SelectedAccount {
            public String id;
            public String name;

            public SelectedAccount(String id, String name) {
              this.id = id;
              this.name = name;
            }
        }
    }
}
