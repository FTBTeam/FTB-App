package net.creeperhost.creeperlauncher.accounts.authentication;

import net.creeperhost.creeperlauncher.accounts.AccountProfile;

public interface AuthenticatorValidator<T, X, Y> {
    /**
     * Validate the users authentication
     */
    boolean isValid(AccountProfile profile);

    Reply<T> refresh(AccountProfile profile, Y refreshData);

    Reply<T> authenticate(X request);

   record Reply<T>(
            T data,
            boolean success,
            String message
    ) {}
}
