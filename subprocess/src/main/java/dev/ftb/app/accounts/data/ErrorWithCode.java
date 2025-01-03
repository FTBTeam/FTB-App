package dev.ftb.app.accounts.data;

public record ErrorWithCode(
    String code, 
    String message
) {}
