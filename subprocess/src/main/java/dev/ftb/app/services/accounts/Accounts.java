package dev.ftb.app.services.accounts;

import dev.ftb.app.AppPaths;
import dev.ftb.app.services.AppHttp;
import dev.ftb.app.services.AppStorage;
import dev.ftb.app.structs.accounts.AccountProvider;
import dev.ftb.app.structs.accounts.LoopbackHolder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Accounts {
    private final McAccounts minecraft;
    private final FtbAccounts ftb;

    // Used to track loopbacks used for auth flows & active flows for device auth.
    private final Map<AccountProvider, LoopbackHolder> loopbacks = Collections.synchronizedMap(new HashMap<>());

    public Accounts(AppPaths paths, AppStorage storage, AppHttp http) {
        this.minecraft = new McAccounts(paths.storageDir(), storage.vault(), http);
        this.ftb = new FtbAccounts(paths.storageDir(), storage.vault(), http);

        this.minecraft.load();
        this.ftb.load();
    }

    public FtbAccounts ftb() {
        return ftb;
    }

    public McAccounts minecraft() {
        return minecraft;
    }

    public Map<AccountProvider, LoopbackHolder> loopbacks() {
        return loopbacks;
    }
}
