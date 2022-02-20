package net.creeperhost.creeperlauncher.accounts.stores;

import java.util.Objects;

public final class AccountSkin {
    private String id;
    private String state;
    private String url;
    private String variant;
    private String alias;

    public AccountSkin(
            String id,
            String state,
            String url,
            String variant,
            String alias
    ) {
        this.id = id;
        this.state = state;
        this.url = url;
        this.variant = variant;
        this.alias = alias;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String toString() {
        return "AccountSkin[" +
                "id=" + id + ", " +
                "state=" + state + ", " +
                "url=" + url + ", " +
                "variant=" + variant + ", " +
                "alias=" + alias + ']';
    }
}
