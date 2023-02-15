package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.other.WebRequestData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import okhttp3.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Created by covers1624 on 30/11/22.
 */
public class WebRequestHandler implements IMessageHandler<WebRequestData> {
    private static final Logger LOGGER = LogManager.getLogger();
        
    @Override
    public void handle(WebRequestData data) {
        Request.Builder builder = new Request.Builder()
                .url(data.url)
                .method(data.method, buildRequestBody(data.body));
        for (Map.Entry<String, List<String>> headerEntry : data.headers.entrySet()) {
            for (String s : headerEntry.getValue()) {
                builder.addHeader(headerEntry.getKey(), s);
            }
        }
        
        try (Response response = Constants.httpClient().newCall(builder.build()).execute()) {
            WebRequestData.WebRequestDataResponse resp = new WebRequestData.WebRequestDataResponse(data, "success");
            resp.statusCode = response.code();
            resp.statusLine = response.message();

            for (Map.Entry<String, List<String>> entry : response.headers().toMultimap().entrySet()) {
                resp.headers.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            ResponseBody body = response.body();
            if (body != null) {
                resp.body = new WebRequestData.Body();
                MediaType contentType = body.contentType();
                if (contentType != null) {
                    resp.body.contentType = contentType.toString();
                }
                resp.body.bytes = body.bytes();
            }
            
            Settings.webSocketAPI.sendMessage(resp);
        } catch (IOException ex) {
            Settings.webSocketAPI.sendMessage(new WebRequestData.WebRequestDataResponse(data, "error", ex.getMessage()));
            LOGGER.warn("Failed to make web request to {}.", data.url, ex);
        }
    }

    @Nullable
    private RequestBody buildRequestBody(WebRequestData.Body body) {
        if (body == null) return null;

        MediaType mediaType = body.contentType != null ? MediaType.parse(body.contentType) : null;
        return RequestBody.create(body.bytes, mediaType);
    }
}
