package net.creeperhost.creeperlauncher.api.data.other;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Makes a web request.
 * <p>
 * Cookies will be handled and stored automatically, they will not be persisted.
 * <p>
 * Created by covers1624 on 30/11/22.
 */
public class WebRequestData extends BaseData {

    public String url;
    public String method;
    public Map<String, List<String>> headers = new HashMap<>();
    @Nullable
    public Body body;

    public static class WebRequestDataResponse extends BaseData {

        // 'success' if the web request was successful, 'error' otherwise.
        public String status;
        // Contains the exception message from the request.
        @Nullable
        public String statusMessage;

        // HTTP status code
        public int statusCode;
        // HTTP status code message
        @Nullable
        public String statusLine;
        public Map<String, List<String>> headers = new HashMap<>();
        @Nullable
        public Body body;

        public WebRequestDataResponse(WebRequestData data, String status) {
            this(data, status, null);
        }

        public WebRequestDataResponse(WebRequestData data, String status, @Nullable String statusMessage) {
            type = "webRequest";
            requestId = data.requestId;
            this.status = status;
            this.statusMessage = statusMessage;
        }
    }

    public static class Body {

        public String contentType;
        public byte[] bytes;
    }
}
