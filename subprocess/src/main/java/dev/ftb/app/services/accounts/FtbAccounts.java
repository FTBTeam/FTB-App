package dev.ftb.app.services.accounts;

import dev.ftb.app.services.AppHttp;
import dev.ftb.app.services.storage.Vault;
import dev.ftb.app.services.storage.VaultException;
import dev.ftb.app.structs.accounts.FtbFinalToken;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/// The signed in FTB accounts
public class FtbAccounts extends MultiAccountStore<FtbAccount, String, FtbAccount.TokenKey> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtbAccounts.class);

    public static final String SPEC_VERSION = "1.0.0";
    private static final long EXPIRY_MARGIN_SECONDS = 60;

    private final FtbTokenExchange tokenExchange;

    /// One lock per account, so only one refresh per account runs at a time
    private final ConcurrentMap<String, Object> refreshLocks = new ConcurrentHashMap<>();

    public FtbAccounts(Path storageDir, Vault vault, AppHttp http) {
        super(
            storageDir,
            vault,
            "ftb-accounts",
            SPEC_VERSION,
            "ftb",
            FtbAccount.class,
            String.class,
            FtbAccount.TokenKey.class,
            FtbAccount::getId
        );
        this.tokenExchange = new FtbTokenExchange(http);
    }

    public FtbTokenExchange tokenExchange() {
        return tokenExchange;
    }

    /// A usable access token for the account, refreshing it first if it has expired or is about to.
    ///
    /// @return `null` if there's no such account, it has to sign in again, or its token has expired
    public @Nullable String accessToken(String id) {
        String token = this.currentIfFresh(id);
        if (token != null) {
            return token;
        }

        synchronized (this.refreshLocks.computeIfAbsent(id, k -> new Object())) {
            // Another caller may have refreshed while this one waited
            token = this.currentIfFresh(id);
            if (token != null) {
                return token;
            }

            return this.refresh(id);
        }
    }

    /// For when an API refused `rejected` from [#accessToken]. Refreshes, unless another caller already has.
    ///
    /// @return a replacement, `null` if there isn't one. May be `rejected` itself when a refresh isn't possible right now
    public @Nullable String accessTokenAfterRejection(String id, String rejected) {
        synchronized (this.refreshLocks.computeIfAbsent(id, k -> new Object())) {
            String current = this.currentIfFresh(id);
            if (current != null && !current.equals(rejected)) {
                return current;
            }

            return this.refresh(id);
        }
    }

    /// Refreshes every signed in account whose access token has expired or is about to
    public void refreshAll() {
        for (FtbAccount account : this.all()) {
            if (!account.isNotLoggedIn()) {
                this.accessToken(account.getId());
            }
        }
    }

    /// Tokens for the active account, for [dev.ftb.app.services.AppHttp] to send to the modpack API
    public ApiTokenSource activeAccountTokens() {
        return new ApiTokenSource() {
            @Override
            public @Nullable String token() {
                FtbAccount active = FtbAccounts.this.active();
                return active == null ? null : FtbAccounts.this.accessToken(active.getId());
            }

            @Override
            public @Nullable String tokenAfterRejection(String rejected) {
                FtbAccount active = FtbAccounts.this.active();
                return active == null ? null : FtbAccounts.this.accessTokenAfterRejection(active.getId(), rejected);
            }
        };
    }

    /// The stored access token, if the account is signed in and the token isn't close to expiring
    private @Nullable String currentIfFresh(String id) {
        FtbAccount account = this.get(id);
        if (account == null || account.isNotLoggedIn() || this.now() >= account.getAccessExpiresAt() - EXPIRY_MARGIN_SECONDS) {
            return null;
        }

        return this.token(id, FtbAccount.TokenKey.ACCESS_TOKEN);
    }

    /// Only call while holding the account's lock
    private @Nullable String refresh(String id) {
        FtbAccount account = this.get(id);
        if (account == null || account.isNotLoggedIn()) {
            return null;
        }

        long now = this.now();
        if (account.getRefreshExpiresAt() != 0 && now >= account.getRefreshExpiresAt()) {
            LOGGER.info("FTB account {} can't be refreshed, its refresh token has expired", id);
            this.signedOut(id);
            return null;
        }

        String current;
        String refreshToken;
        try {
            current = this.readToken(id, FtbAccount.TokenKey.ACCESS_TOKEN);
            refreshToken = this.readToken(id, FtbAccount.TokenKey.REFRESH_TOKEN);
        } catch (VaultException e) {
            // Could be a locked keychain, so don't sign the account out over it
            LOGGER.error("Couldn't read the tokens for FTB account {}", id, e);
            return null;
        }

        if (refreshToken == null) {
            LOGGER.warn("FTB account {} has no refresh token, it has to sign in again", id);
            this.signedOut(id);
            return null;
        }

        return switch (this.tokenExchange.refresh(refreshToken)) {
            case TokenExchange.RefreshResult.Refreshed<FtbFinalToken>(var token) -> {
                try {
                    // The vault first, the provider may already have invalidated the old refresh token
                    this.storeTokens(id, Map.of(
                        FtbAccount.TokenKey.ACCESS_TOKEN, token.accessToken(),
                        FtbAccount.TokenKey.REFRESH_TOKEN, token.refreshToken()
                    ));
                } catch (VaultException e) {
                    // Still usable for now. The old expiry is kept so the next call tries again.
                    LOGGER.error("Couldn't save the refreshed tokens for FTB account {}", id, e);
                    yield token.accessToken();
                }

                long refreshedAt = this.now();
                this.update(id, a -> a.refreshed(
                    refreshedAt + token.expiresIn(),
                    token.refreshExpiresIn() <= 0 ? 0 : refreshedAt + token.refreshExpiresIn()
                ));
                yield token.accessToken();
            }
            case TokenExchange.RefreshResult.Rejected<FtbFinalToken>(var detail) -> {
                this.signedOut(id);
                yield null;
            }
            case TokenExchange.RefreshResult.Unavailable<FtbFinalToken>(var reason) -> {
                LOGGER.warn("Couldn't refresh FTB account {} right now: {}", id, reason);
                // Fall back to the old token while it's still technically valid
                yield current != null && now < account.getAccessExpiresAt() ? current : null;
            }
        };
    }

    private void signedOut(String id) {
        this.update(id, FtbAccount::markNotLoggedIn);
    }

    private long now() {
        return Instant.now().getEpochSecond();
    }
}
