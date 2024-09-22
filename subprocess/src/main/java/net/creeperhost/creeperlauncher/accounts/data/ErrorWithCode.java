package net.creeperhost.creeperlauncher.accounts.data;

public record ErrorWithCode(
    String code, 
    String message
) {}
