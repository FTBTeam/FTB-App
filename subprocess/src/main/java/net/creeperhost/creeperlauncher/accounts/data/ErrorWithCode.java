package net.creeperhost.creeperlauncher.accounts.data;

public record ErrorWithCode(
        String error,
        String code
) {
    public String join() {
        return this.code + "|" + this.code;
    }
}
