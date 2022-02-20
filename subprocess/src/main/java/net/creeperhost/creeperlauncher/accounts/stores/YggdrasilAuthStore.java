package net.creeperhost.creeperlauncher.accounts.stores;

import net.creeperhost.creeperlauncher.accounts.AccountProfile;

import java.util.StringJoiner;

public class YggdrasilAuthStore {
    public String clientToken;
    public String accessToken;

    public YggdrasilAuthStore(String accessToken, String clientToken) {
        this.clientToken = clientToken;
        this.accessToken = accessToken;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", YggdrasilAuthStore.class.getSimpleName() + "[", "]")
                .add("clientToken=" + clientToken)
                .add("accessToken='" + accessToken + "'")
                .toString();
    }
}
