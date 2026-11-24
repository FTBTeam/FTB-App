package dev.ftb.app.services.accounts.minecraft;

import com.google.gson.Gson;
import dev.ftb.app.services.AppHttp;
import dev.ftb.app.services.accounts.TokenExchange;
import dev.ftb.app.structs.accounts.MicrosoftFinalToken;
import dev.ftb.app.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The Microsoft (Entra ID, consumer accounts) device code flow, used to get the Microsoft token that the Xbox Live
/// and Minecraft steps in [MicrosoftOAuthProcess] start from.
///
/// [See](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code)
public class MicrosoftTokenExchange extends TokenExchange<MicrosoftFinalToken> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftTokenExchange.class);
    private static final Gson GSON = new Gson();

    public static final String CLIENT_ID = "f23e8ba8-f46b-41ed-b5c0-7994f2ebbbf8";
    private static final String SCOPE = "offline_access XboxLive.signin";
    private static final String AUTHORITY = "https://login.microsoftonline.com/consumers/oauth2/v2.0";

    public MicrosoftTokenExchange(AppHttp appHttp) {
        this(appHttp, AUTHORITY);
    }

    /// For tests, `authority` replaces the `.../oauth2/v2.0` base
    MicrosoftTokenExchange(AppHttp appHttp, String authority) {
        super(LOGGER, GSON, appHttp, "Microsoft", MicrosoftFinalToken.class, authority + "/authorize", authority + "/devicecode", authority + "/token", CLIENT_ID, SCOPE);
    }

    /// Lists the Microsoft accounts the browser remembers, plus "use another account".
    @Override
    protected String chooseAccountPrompt() {
        return "select_account";
    }

    @Override
    protected boolean isTokenValid(MicrosoftFinalToken token) {
        // No refresh token means the account can't be kept signed in
        return !StringUtils.isBlank(token.accessToken()) && !StringUtils.isBlank(token.refreshToken());
    }
}
