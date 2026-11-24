package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.accounts.LoginFlowEvent;
import dev.ftb.app.services.Services;
import dev.ftb.app.services.accounts.FtbAccount;
import dev.ftb.app.services.accounts.FtbTokenClaims;
import dev.ftb.app.services.accounts.minecraft.MicrosoftOAuthProcess;
import dev.ftb.app.services.accounts.minecraft.MicrosoftProfile;
import dev.ftb.app.services.storage.VaultException;
import dev.ftb.app.structs.accounts.AccountProvider;
import dev.ftb.app.structs.accounts.CodedError;
import dev.ftb.app.structs.accounts.FtbFinalToken;
import dev.ftb.app.structs.accounts.MicrosoftFinalToken;
import dev.ftb.app.util.MiscUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/// Turns the tokens from a finished browser or device code login into a saved account
final class LoginFinisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginFinisher.class);

    private final Services services;
    private final AccountProvider provider;
    private final String flowId;

    private LoginFinisher(Services services, AccountProvider provider, String flowId) {
        this.services = services;
        this.provider = provider;
        this.flowId = flowId;
    }

    static void working(Services services, AccountProvider provider, String flowId, LoginFlowEvent.LoginStep step) {
        new LoginFinisher(services, provider, flowId).working(step);
    }

    static void failed(Services services, AccountProvider provider, String flowId, String message) {
        new LoginFinisher(services, provider, flowId).failed(message);
    }

    /// Saves the account `token` belongs to. If the token is a microsoft account, more work is required
    /// so we push that data back as events.
    static void finish(Services services, AccountProvider provider, String flowId, Object token) {
        var finisher = new LoginFinisher(services, provider, flowId);
        try {
            switch (token) {
                case FtbFinalToken ftb -> finisher.finishFtb(ftb);
                case MicrosoftFinalToken microsoft -> finisher.finishMicrosoft(microsoft);
                default -> {
                    LOGGER.error("Unexpected token type {} for a {} login", token.getClass().getName(), provider);
                    finisher.failed("Something went wrong finishing the login. Please try again.");
                }
            }
        } catch (RuntimeException e) {
            // Always end with an event, or the frontend waits forever
            LOGGER.error("Failed to finish a {} login", provider, e);
            finisher.failed("Something went wrong finishing the login. Please try again.");
        }
    }

    private void finishFtb(FtbFinalToken token) {
        this.working(LoginFlowEvent.LoginStep.SAVING);
        
        FtbTokenClaims claims = FtbTokenClaims.read(token.accessToken());
        if (claims == null) {
            LOGGER.error("FTB access token had no readable sub claim");
            this.failed("FTB returned an account we couldn't read. Please try again.");
            return;
        }

        String id = claims.id();
        long now = Instant.now().getEpochSecond();
        var account = new FtbAccount(
            claims,
            now + token.expiresIn(),
            // I don't think this can actually happen...
            token.refreshExpiresIn() <= 0 ? 0 : now + token.refreshExpiresIn()
        );

        var accounts = this.services.accounts().ftb();
        try {
            accounts.storeTokens(id, Map.of(
                FtbAccount.TokenKey.ACCESS_TOKEN, token.accessToken(),
                FtbAccount.TokenKey.REFRESH_TOKEN, token.refreshToken()
            ));
        } catch (VaultException e) {
            LOGGER.error("Failed to store FTB account tokens", e);
            this.failed("Couldn't securely save your FTB account. Please try again.");
            return;
        }

        if (!accounts.add(account)) {
            this.failed("Couldn't save your FTB account. Please try again.");
            return;
        }

        this.complete(claims.username());
    }

    // Run over the Microsoft flow...
    private void finishMicrosoft(MicrosoftFinalToken token) {
        var microsoftToken = token.toHolder();
        var result = MicrosoftOAuthProcess.authWithMinecraft(this.services.http(), microsoftToken, stage -> this.working(switch (stage) {
            case XBOX -> LoginFlowEvent.LoginStep.XBOX;
            case XSTS -> LoginFlowEvent.LoginStep.XSTS;
            case MINECRAFT -> LoginFlowEvent.LoginStep.MINECRAFT;
            case PROFILE -> LoginFlowEvent.LoginStep.PROFILE;
        }));

        if (result.isErr()) {
            var error = result.unwrapErr();
            LOGGER.error("Failed to sign in to Minecraft: {} ({})", error.message(), error.code());
            this.failed(error.message(), error.code());
            return;
        }

        this.working(LoginFlowEvent.LoginStep.SAVING);

        var data = result.unwrap();
        var accounts = this.services.accounts().minecraft();
        var uuid = MiscUtils.createUuidFromStringWithoutDashes(data.profileData().id());
        try {
            accounts.storeTokens(uuid, Map.of(
                MicrosoftProfile.TokenKey.MINECRAFT_ACCESS_TOKEN, data.minecraftAccessToken(),
                MicrosoftProfile.TokenKey.MICROSOFT_REFRESH_TOKEN, microsoftToken.refreshToken()
            ));
        } catch (VaultException e) {
            LOGGER.error("Failed to store the Minecraft account tokens in the vault", e);
            this.failed("Couldn't securely save your Minecraft account. Please try again.", CodedError.TOKEN_STORAGE_FAILED.code);
            return;
        }

        var profile = new MicrosoftProfile(
            uuid,
            data.profileData().name(),
            Instant.now().getEpochSecond() + data.minecraftExpiresIn(),
            data.userHash(),
            data.profileData().getActiveSkinUrl()
        );

        if (!accounts.add(profile)) {
            this.failed("Couldn't save your Minecraft account. Please try again.");
            return;
        }

        this.complete(data.profileData().name());
    }

    private void working(LoginFlowEvent.LoginStep step) {
        this.services.rcpApi().sendMessage(LoginFlowEvent.working(this.provider, this.flowId, step));
    }

    private void complete(String accountName) {
        this.services.rcpApi().sendMessage(LoginFlowEvent.complete(this.provider, this.flowId, accountName));
    }

    private void failed(String message) {
        this.failed(message, null);
    }

    private void failed(String message, @Nullable String errorCode) {
        this.services.rcpApi().sendMessage(LoginFlowEvent.failed(this.provider, this.flowId, message, errorCode));
    }
}
