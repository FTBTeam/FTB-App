package net.creeperhost.creeperlauncher.accounts.data;

public record AccountSkin(String id, String state, String url, String variant, String alias) {
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
