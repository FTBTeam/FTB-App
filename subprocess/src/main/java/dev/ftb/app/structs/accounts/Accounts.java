package dev.ftb.app.structs.accounts;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/// Underlying data type used for the [dev.ftb.app.services.accounts.MultiAccountStore]
///
/// @param <T> the account type
/// @param <I> the type of the account's id
public class Accounts<T, I> {
    /// A fresh store each time, as the profiles set is mutated in place.
    public static <T, I> Accounts<T, I> empty(String version) {
        return new Accounts<>(version, new LinkedHashSet<>(), null);
    }

    String version;

    public Set<T> profiles;

    @Nullable
    public I activeProfile;

    public Accounts(String version, Set<T> profiles, @Nullable I activeProfile) {
        this.version = version;
        this.profiles = profiles;
        this.activeProfile = activeProfile;
    }
}
