package dev.ftb.app.services.accounts;

import com.google.gson.Gson;
import dev.ftb.app.services.AppHttp;
import dev.ftb.app.structs.accounts.FtbFinalToken;
import dev.ftb.app.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The FTB identity server
///
/// [See](https://www.keycloak.org/securing-apps/oidc-layers#_device_authorization_endpoint)
public class FtbTokenExchange extends TokenExchange<FtbFinalToken> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtbTokenExchange.class);
    private static final Gson GSON = new Gson();

    private static final String AUTHORITY = "https://identity.feed-the-beast.com/realms/FTB/protocol/openid-connect";
    public static final String CLIENT_ID = "ftb-app";
    private static final String SCOPE = "openid email profile offline_access";

    public FtbTokenExchange(AppHttp http) {
        this(http, AUTHORITY);
    }
    
    FtbTokenExchange(AppHttp http, String authority) {
        super(LOGGER, GSON, http, "FTB", FtbFinalToken.class, authority + "/auth", authority + "/auth/device", authority + "/token", CLIENT_ID, SCOPE);
    }

    /// `login` always shows the sign-in form,
    @Override
    protected String chooseAccountPrompt() {
        return "login";
    }

    @Override
    protected boolean isTokenValid(FtbFinalToken token) {
        return !StringUtils.isBlank(token.accessToken())
            && !StringUtils.isBlank(token.idToken())
            && !StringUtils.isBlank(token.refreshToken());
    }
}
