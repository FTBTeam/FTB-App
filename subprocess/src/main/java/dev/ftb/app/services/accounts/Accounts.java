package dev.ftb.app.services.accounts;

import dev.ftb.app.AppPaths;
import dev.ftb.app.services.AppStorage;

public final class Accounts {
    private final McAccounts minecraft;
    private final FtbAccounts ftb;

    public Accounts(AppPaths paths, AppStorage storage) {
        this.minecraft = new McAccounts(paths, storage);
        this.ftb = new FtbAccounts(paths, storage);
    }

    public FtbAccounts ftb() {
        return ftb;
    }

    public McAccounts minecraft() {
        return minecraft;
    }
}
