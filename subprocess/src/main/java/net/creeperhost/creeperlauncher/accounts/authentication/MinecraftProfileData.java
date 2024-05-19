package net.creeperhost.creeperlauncher.accounts.authentication;

import net.creeperhost.creeperlauncher.accounts.data.AccountSkin;

import java.util.List;

public record MinecraftProfileData(
    String id,
    String name,
    List<AccountSkin> skins,
    List<Capes> capes,
    Object profileActions
) {
    record Capes(
        String id,
        String state,
        String url,
        String alias
    ){}
}


