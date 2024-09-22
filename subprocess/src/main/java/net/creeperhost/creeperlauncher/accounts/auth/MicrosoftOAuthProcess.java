package net.creeperhost.creeperlauncher.accounts.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.accounts.data.*;
import net.creeperhost.creeperlauncher.util.Result;

public class MicrosoftOAuthProcess {
    private static final Gson GSON = new Gson();
    
    /**
     * Runs over the entire Microsoft authentication flow. This is quite a few steps and can take a bit to complete fully.
     */
    public static Result<MinecraftProfileWithAuthData, ErrorWithCode> authWithMinecraft(OAuthTokenHolder xboxTokenHolder) {
        var stepOne = MicrosoftRequests.authenticateWithXbox(xboxTokenHolder.accessToken());
        if (stepOne.isErr()) {
            return Result.err(stepOne.unwrapErr());
        }
        
        var stepTwo = MicrosoftRequests.authenticateWithXSTS(stepOne.unwrap().get("Token").getAsString());
        if (stepTwo.isErr()) {
            return Result.err(stepTwo.unwrapErr());
        }

        JsonObject stepTwoData = stepTwo.unwrap();
        if (!stepTwoData.has("DisplayClaims")) {
            return Result.err(CodedError.MISSING_CLAIMS.toError());
        }
        
        var displayClaims = stepTwoData.get("DisplayClaims").getAsJsonObject();
        var notAfter = stepTwoData.get("NotAfter").getAsString();
        var xstsToken = stepTwoData.get("Token").getAsString();
        
        Result<String, ErrorWithCode> userHashRes = getUserHashFromDisplayClaims(displayClaims);
        if (userHashRes.isErr()) {
            return Result.err(userHashRes.unwrapErr());
        }
        
        String userHash = userHashRes.unwrap();
        
        var stepThree = MicrosoftRequests.authenticateWithMinecraft(
            stepTwoData.get("Token").getAsString(),
            userHash
        );
        
        if (stepThree.isErr()) {
            return Result.err(stepThree.unwrapErr());
        }
        
        var minecraftAccessToken = stepThree.unwrap().get("access_token").getAsString();
        var minecraftExpiresIn = stepThree.unwrap().get("expires_in").getAsInt();
        
        var profile = checkEntitlementsAndGetProfile(minecraftAccessToken);
        if (profile.isErr()) {
            return Result.err(profile.unwrapErr());
        }
        
        return Result.ok(new MinecraftProfileWithAuthData(
            profile.unwrap(),
            minecraftAccessToken,
            minecraftExpiresIn,
            xboxTokenHolder,
            userHash,
            notAfter,
            xstsToken
        ));
    }
    
    private static boolean validateEntitlements(JsonObject entitlements) {
        var items = entitlements.getAsJsonArray("items");
        
        boolean hasProductMinecraft = false;
        boolean hasGameMinecraft = false;
        
        for (var item : items) {
            if (item.getAsJsonObject().get("name").getAsString().equals("product_minecraft")) {
                hasProductMinecraft = true;
            }
            
            if (item.getAsJsonObject().get("name").getAsString().equals("game_minecraft")) {
                hasGameMinecraft = true;
            }
        }

        // Does the account own minecraft? We check this by looking for the product_minecraft and game_minecraft entitlements
        // Either one of these should be present to continue
        return hasProductMinecraft || hasGameMinecraft;
    }
    
    public static Result<MinecraftProfileData, ErrorWithCode> checkEntitlementsAndGetProfile(String minecraftAccessToken) {
        var entitlements = MicrosoftRequests.queryEntitlements(minecraftAccessToken);
        if (entitlements.isErr()) {
            return Result.err(entitlements.unwrapErr());
        }

        // Validate the entitlements
        var hasGame = validateEntitlements(entitlements.unwrap());
        if (!hasGame) {
            return Result.err(CodedError.MISSING_ENTITLEMENTS.toError());
        }

        var profile = MicrosoftRequests.queryProfile(minecraftAccessToken);
        if (profile.isErr()) {
            return Result.err(profile.unwrapErr());
        }
        
        try {
            return Result.ok(GSON.fromJson(profile.unwrap(), MinecraftProfileData.class));
        } catch (Exception e) {
            return Result.err(CodedError.INVALID_PROFILE.toError());
        }
    }
    
    public static Result<String, ErrorWithCode> getUserHashFromDisplayClaims(JsonObject displayClaims) {
        String userHash;
        try {
            userHash = displayClaims.get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
        } catch (Exception e) {
            return Result.err(CodedError.MISSING_CLAIMS.toError());
        }
        
        return Result.ok(userHash);
    }
}
