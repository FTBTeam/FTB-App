package dev.ftb.app.accounts.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.ftb.app.Constants;
import dev.ftb.app.accounts.data.CodedError;
import dev.ftb.app.accounts.data.ErrorWithCode;
import dev.ftb.app.util.Result;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class MicrosoftRequests {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftRequests.class);
    private static final Gson GSON = new Gson();
    
    private static final Request.Builder STANDARD_REQ = new Request.Builder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");

    public static Result<JsonObject, ErrorWithCode> refreshWithXbox(String refreshToken) {
        return standardJsonReq(
            STANDARD_REQ
                .url("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
                .post(RequestBody.create(
                    "client_id=f23e8ba8-f46b-41ed-b5c0-7994f2ebbbf8" +
                        "&scope=" + "offline_access%20XboxLive.signin" +
                        "&refresh_token=" + refreshToken +
                        "&grant_type=refresh_token",
                    MediaType.parse("application/x-www-form-urlencoded")
                )),
            json -> json.has("access_token") && json.has("refresh_token")
        );
    }
    
    /**
     * Authenticate with Microsoft using an Xbox access token
     */
    public static Result<JsonObject, ErrorWithCode> authenticateWithXbox(String accessToken) {
        return standardJsonReq(
            STANDARD_REQ
                .url("https://user.auth.xboxlive.com/user/authenticate")
                .post(mapToJsonBody(Map.of(
                    "Properties", Map.of(
                        "AuthMethod", "RPS",
                        "SiteName", "user.auth.xboxlive.com",
                        "RpsTicket", "d=" + accessToken
                    ),
                    "RelyingParty", "http://auth.xboxlive.com",
                    "TokenType", "JWT"
                ))),
            json -> json.has("Token")
        );
    }

    /**
     * Authenticate with XSTS using a token from the previous step
     */
    public static Result<JsonObject, ErrorWithCode> authenticateWithXSTS(String token) {
        var res = standardJsonReq(
            STANDARD_REQ
                .url("https://xsts.auth.xboxlive.com/xsts/authorize")
                .post(mapToJsonBody(Map.of(
                    "Properties", Map.of(
                        "SandboxId", "RETAIL",
                        "UserTokens", new String[] { token }
                    ),
                    "RelyingParty", "rp://api.minecraftservices.com/",
                    "TokenType", "JWT"
                ))),
            json -> (json.has("Token") && json.has("DisplayClaims")) || json.has("XErr"),
            true
        );
        
        var json = res.unwrap();
        if (json.has("XErr")) {
            // Parse the error code and return it
            LOGGER.error("Failed to authenticate with XSTS: {}", json);
            return Result.err(decipherXErr(json.get("XErr").getAsString()).toError());
        }
        
        return res;
    }

    public static Result<JsonObject, ErrorWithCode> authenticateWithMinecraft(String token, String userHash) {
        return standardJsonReq(
            STANDARD_REQ
                .url("https://api.minecraftservices.com/launcher/login")
                .post(mapToJsonBody(Map.of(
                    "platform", "PC_LAUNCHER",
                    "xtoken", String.format("XBL3.0 x=%s;%s", userHash, token)
                ))),
            json -> json.has("access_token")
        );
    }

    public static Result<JsonObject, ErrorWithCode> queryEntitlements(String accessToken) {
        return standardJsonReq(
            STANDARD_REQ
                .url("https://api.minecraftservices.com/entitlements/license?requestId=" + UUID.randomUUID())
                .header("Authorization", "Bearer " + accessToken),
            json -> json.has("items")
        );
    }
    
    public static Result<JsonObject, ErrorWithCode> queryProfile(String accessToken) {
        return standardJsonReq(
            STANDARD_REQ
                .url("https://api.minecraftservices.com/minecraft/profile")
                .header("Authorization", "Bearer " + accessToken)
                .get(),
            json -> json.has("id") && json.has("name")
        );
    }
    
    /**
     * Attempt to figure out why the user might not be allowed to log in with xbox
     */
    private static CodedError decipherXErr(String xerr) {
        return switch (xerr) {
            case "2148916233" -> CodedError.XBOX_ACCOUNT_MISSING;
            case "2148916235" -> CodedError.XBOX_INVALID_REGION;
            case "2148916236", "2148916237" -> CodedError.XBOX_ADULT_VERIFICATION;
            case "2148916238" -> CodedError.XBOX_UNDER_18;
            default -> CodedError.XBOX_UNKNOWN_FAILURE_CODE;
        };
    }
    
    private static Result<JsonObject, ErrorWithCode> standardJsonReq(Request.Builder builder, Function<JsonObject, Boolean> validator) {
        return standardJsonReq(builder, validator, false);
    }
    
    /**
     * Runs a standard request with post 
     */
    private static Result<JsonObject, ErrorWithCode> standardJsonReq(
        Request.Builder builder,
        Function<JsonObject, Boolean> validator,
        boolean bypassIsSuccessful
    ) {
        var response = request(builder);

        if (response.isErr()) {
            return Result.err(logAndCreateError(CodedError.COMPLETE_REQUEST_FAILURE));
        }

        try (var responseRaw = response.unwrap()) {
            ResponseBody resBody = responseRaw.body();
            if (resBody == null) {
                return Result.err(logAndCreateError(CodedError.MISSING_BODY));
            }

            if (!responseRaw.isSuccessful() && !bypassIsSuccessful) {
                return Result.err(logAndCreateError(CodedError.NOT_SUCCESSFUL)); 
            }
            
            var body = resBody.string();
            if (!body.startsWith("{")) {
                LOGGER.error("Response was not a JSON object: {}", body);
                return Result.err(logAndCreateError(CodedError.WRONG_BODY_TYPE));
            }

            // Parse the response to a JsonObj
            var json = GSON.fromJson(body, JsonObject.class);
            if (json == null) {
                return Result.err(logAndCreateError(CodedError.BODY_PARSE_FAILED));
            }
            
            if (!validator.apply(json)) {
                // Print the response and return a validation error
                LOGGER.error("Failed to validate response: {}", json);
                
                return Result.err(logAndCreateError(CodedError.VALIDATION_FAILED));
            }
            
            return Result.ok(json);
        } catch (IOException e) {
            LOGGER.error("Failed to read response body", e);
            return Result.err(logAndCreateError(CodedError.BODY_READING_FAILED));
        }
    }

    /**
     * Log wrapped request data and execute the request
     */
    private static Result<Response, String> request(Request.Builder builder) {
        var client = Constants.httpClient();
        var request = builder.build();
        
        logRequestData(request);
        
        try {
            var responseRaw = client.newCall(request).execute();
            logResponse(responseRaw);
            
            return Result.ok(responseRaw);
        } catch (IOException e) {
            LOGGER.error("Failed to execute request to {}", request.url(), e);
        }
        
        return Result.err("Failed to execute request");
    }

    /**
     * Convert a map to a JSON request body, fails if the map cannot be converted
     */
    private static RequestBody mapToJsonBody(Map<?, ?> obj) {
        try {
            var json = GSON.toJson(obj);
            return RequestBody.create(json, MediaType.parse("application/json"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void logRequestData(Request request) {
        LOGGER.info("Requesting: {}", request.url());
        logHeaders("Request", request.headers());
    }
    
    private static void logResponse(Response response) {
        LOGGER.info("Response Code: {}", response.code());
        String message = response.message();
        if (!message.isEmpty()) {
            LOGGER.info("Response Message: {}", message);
        }
        
        logHeaders("Response", response.headers());
    }
    
    private static void logHeaders(String direction, Headers headers) {
        StringBuilder headerString = new StringBuilder();
        headerString.append(direction).append(" ").append("Headers: ");
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            String value = headers.value(i);
            if (name.toLowerCase().contains("authorization")) {
                value = "REDACTED";
            }

            headerString.append("\t").append(name).append(" = ").append(value).append("\n");
        }
        
        LOGGER.info(headerString.toString());
    }
    
    private static ErrorWithCode logAndCreateError(CodedError error) {
        LOGGER.error("Error {}: {}", error.code, error.getMessage());
        return error.toError();
    }
}
