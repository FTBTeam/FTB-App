package net.creeperhost.creeperlauncher.accounts.authentication;

import net.creeperhost.creeperlauncher.accounts.AccountProfile;

public interface AuthenticatorValidator<T, X, Y> {
    /**
     * Validate the users authentication
     */
    boolean isValid(AccountProfile profile);

    T refresh(AccountProfile profile, Y refreshData);

    T authenticate(X request);
}
