package net.creeperhost.creeperlauncher.accounts;

public record AccountSkins(
        String id,
        String name,
        AccountSkins.Skin[] skins
) {
    record Skin(
            String id,
            String state,
            String url,
            String variant,
            String alias
    ) { }
}
