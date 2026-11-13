package dev.ftb.app.services.accounts;

/// A kind of token an account keeps in the vault
public interface AccountTokenKey {
    /// The name used in the vault key, `<prefix>.<account id>.<key name>`. Changing it orphans tokens that are
    /// already stored.
    String getKeyName();
}
