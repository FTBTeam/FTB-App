package net.creeperhost.creeperlauncher.accounts.authentication;

import net.creeperhost.creeperlauncher.accounts.AccountProfile;

interface AuthenticatorValidator<T, X, Y> {
    /**
     * Validate the users authentication
     */
    boolean isValid(AccountProfile profile);

    AuthenticatedWithData<T> refresh(AccountProfile profile, Y refreshData);

    AuthenticatedWithData<T> authenticate(X request);

    record AuthenticatedWithData<T>(
            T data,
            boolean success,
            String message
    ) {}
}
