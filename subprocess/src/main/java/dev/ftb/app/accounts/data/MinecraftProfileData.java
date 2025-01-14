package dev.ftb.app.accounts.data;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

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
    
    @Nullable
    public String getActiveSkinUrl() {
        return this.skins()
            .stream()
            .filter(e -> Objects.equals(e.state(), "ACTIVE"))
            .findFirst()
            .map(AccountSkin::url)
            .orElse(null);
    }
}


