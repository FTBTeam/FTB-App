package net.creeperhost.creeperlauncher.accounts.data;

import org.jetbrains.annotations.Nullable;

public record ErrorWithCode(
        String error,
        String code,
        @Nullable StepReply rawReply
) {
    public ErrorWithCode(String error, String code) {
        this(error, code, null);
    }

    public String join() {
        return this.code + "|" + this.code;
    }
}
