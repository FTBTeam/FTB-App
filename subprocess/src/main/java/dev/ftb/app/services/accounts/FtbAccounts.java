package dev.ftb.app.services.accounts;

import dev.ftb.app.services.AppHttp;
import dev.ftb.app.services.storage.Vault;

import java.nio.file.Path;

/// The signed in FTB accounts, keyed by Keycloak user id.
///
/// Not used by the FTB account handlers yet, they still read and write the single legacy `ftbAccount` vault entry.
public class FtbAccounts extends MultiAccountStore<FtbAccount, String, FtbAccount.TokenKey> {
    public static final String SPEC_VERSION = "1.0.0";

    private final FtbTokenExchange tokenExchange;

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
}
