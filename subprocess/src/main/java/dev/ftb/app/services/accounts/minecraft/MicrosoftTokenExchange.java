package dev.ftb.app.services.accounts.minecraft;

import com.google.gson.Gson;
import dev.ftb.app.AppFlags;
import dev.ftb.app.services.AppHttp;
import dev.ftb.app.services.accounts.FtbTokenExchange;
import dev.ftb.app.services.accounts.TokenExchange;
import dev.ftb.app.structs.accounts.DeviceAuthResponse;
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
        super(LOGGER, GSON, appHttp, "Microsoft", MicrosoftFinalToken.class, authority + "/devicecode", authority + "/token", CLIENT_ID, SCOPE);
    }

    @Override
    protected boolean isTokenValid(MicrosoftFinalToken token) {
        // No refresh token means the account can't be kept signed in
        return !StringUtils.isBlank(token.accessToken()) && !StringUtils.isBlank(token.refreshToken());
    }
    
    static void main(String[] args) throws InterruptedException {
        var http = new AppHttp(new AppFlags(true, AppFlags.FrontendPlatform.ELECTRON), () -> Long.MAX_VALUE);
        TokenExchange<?> exchange = args.length > 0 && args[0].equalsIgnoreCase("ftb")
            ? new FtbTokenExchange(http)
            : new MicrosoftTokenExchange(http);

        var initiate = exchange.initiate();
        if (initiate.isErr()) {
            System.err.println("Failed to initiate: " + initiate.unwrapErr());
            return;
        }

        DeviceAuthResponse code = initiate.unwrap();
        System.out.printf("Go to %s and enter %s%n", code.verificationUriComplete() != null ? code.verificationUriComplete() : code.verificationUri(), code.userCode());

        long deadline = System.currentTimeMillis() + code.expiresIn() * 1000L;
        int interval = code.intervalOrDefault();

        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(interval * 1000L);

            switch (exchange.poll(code.deviceCode())) {
                case TokenExchange.PollState.Pending<?>(var slowDown, var retry, var detail) -> {
                    if (slowDown) {
                        interval += 5;
                    }
                    System.out.printf("Pending (slowDown=%s, retry=%s, detail=%s)%n", slowDown, retry, detail);
                }
                case TokenExchange.PollState.Done<?>(var token) -> {
                    System.out.println("Got token: " + token);
                    return;
                }
                case TokenExchange.PollState.Failed<?>(var message, var detail) -> {
                    System.out.printf("Failed: %s (%s)%n", message, detail);
                    return;
                }
            }
        }

        System.out.println("Code expired before the user signed in.");
    }
}
