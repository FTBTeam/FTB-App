package dev.ftb.app.services.accounts;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import dev.ftb.app.services.storage.Vault;
import dev.ftb.app.services.storage.VaultException;
import dev.ftb.app.structs.accounts.Accounts;
import dev.ftb.app.util.JsonStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/// A set of signed-in accounts of one kind, at most one of which is active.
///
/// @param <T> the account type, saved to JSON with Gson
/// @param <I> the account's id type, must have a meaningful `equals`
/// @param <K> the token keys an account can have in the vault
public abstract class MultiAccountStore<T, I, K extends Enum<K> & AccountTokenKey> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String name;
    private final String specVersion;
    private final String vaultPrefix;
    private final Function<T, I> idOf;
    private final Class<K> tokenKeyType;
    private final Vault vault;

    private final JsonStore<Accounts<T, I>> store;
    
    protected MultiAccountStore(
        Path storageDir,
        Vault vault,
        String name,
        String specVersion,
        String vaultPrefix,
        Class<T> accountType,
        Class<I> idType,
        Class<K> tokenKeyType,
        Function<T, I> idOf
    ) {
        this.name = name;
        this.specVersion = specVersion;
        this.vaultPrefix = vaultPrefix;
        this.idOf = idOf;
        this.tokenKeyType = tokenKeyType;
        this.vault = vault;

        Type type = TypeToken.getParameterized(Accounts.class, accountType, idType).getType();
        this.store = new JsonStore<>(
            storageDir.resolve(name + ".json"),
            GSON,
            type,
            () -> Accounts.empty(specVersion),
            this::migrator
        );
    }

    /// Loads the accounts from disk, then runs [#onLoad()]. Falls back to no accounts if the file can't be read.
    public void load() {
        try {
            var result = this.store.load();
            LOGGER.info("Loaded {}: {}", this.name, result);
        } catch (IOException e) {
            LOGGER.error("Failed to load accounts for {}", this.name, e);
            return;
        }

        // A file without a profiles field would otherwise leave this null
        try {
            this.store.updateIf(data -> data.profiles == null, data -> data.profiles = new LinkedHashSet<>());
        } catch (IOException e) {
            LOGGER.error("Failed to save {} after repairing it", this.name, e);
        }
        
        this.onLoad();
    }

    /// Hook for after load effects, migrations specifically. Runs once per [#load()], after the file has been read
    /// and [#runMigrator] has run.
    protected void onLoad() {}

    /// The accounts
    public ImmutableList<T> all() {
        return ImmutableList.copyOf(this.store.get().profiles);
    }
    
    @Nullable
    public T get(I id) {
        return find(this.store.get(), id);
    }

    /// The active account, or `null` if there isn't one
    @Nullable
    public T active() {
        var data = this.store.get();
        return data.activeProfile == null ? null : find(data, data.activeProfile);
    }

    /// Adds the account and makes it active. An existing account with the same id is replaced, its tokens are left
    /// alone, so store the new tokens with [#storeTokens] first.
    ///
    /// @return false if the accounts couldn't be saved, nothing is changed then
    public boolean add(T account) {
        I id = this.idOf.apply(account);
        try {
            this.store.update(data -> {
                data.profiles.removeIf(p -> id.equals(this.idOf.apply(p)));
                data.profiles.add(account);
                data.activeProfile = id;
            });
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save {} after adding an account", this.name, e);
            return false;
        }
    }

    /// Changes an existing account and saves it. `mutator` gets a private copy of the account, so changing it is safe.
    ///
    /// @return false if there's no such account, or the change couldn't be saved
    public boolean update(I id, Consumer<T> mutator) {
        try {
            return this.store.updateIf(
                data -> find(data, id) != null,
                data -> mutator.accept(Objects.requireNonNull(find(data, id)))
            );
        } catch (IOException e) {
            LOGGER.error("Failed to save {} after updating account {}", this.name, id, e);
            return false;
        }
    }

    /// Removes the account and all of its tokens. If it was active, another account (if any) becomes active.
    ///
    /// The tokens are removed even if there was no such account, so stray tokens can be cleaned up.
    ///
    /// @return whether the account existed and its removal was saved
    public boolean remove(I id) {
        boolean removed;
        try {
            removed = this.store.updateIf(
                data -> find(data, id) != null,
                data -> {
                    data.profiles.removeIf(p -> id.equals(this.idOf.apply(p)));
                    if (id.equals(data.activeProfile)) {
                        data.activeProfile = data.profiles.stream().findFirst().map(this.idOf).orElse(null);
                    }
                }
            );
        } catch (IOException e) {
            LOGGER.error("Failed to save {} after removing an account", this.name, e);
            removed = false;
        }

        this.removeTokens(id);
        return removed;
    }

    /// Makes the account active. If there's no such account, nothing is active afterwards.
    ///
    /// @return whether the account exists and the change was saved
    public boolean setActive(I id) {
        try {
            // The existence check runs on the same data the change is applied to, so a concurrent remove can't
            // leave a deleted account active
            this.store.update(data -> data.activeProfile = find(data, id) != null ? id : null);
            return id.equals(this.store.get().activeProfile);
        } catch (IOException e) {
            LOGGER.error("Failed to save {} after changing the active account", this.name, e);
            return false;
        }
    }

    /// Writes the accounts to disk again without changing them. Only useful to push an old version out of the
    /// backup file, changes are saved by the methods that make them.
    public void save() {
        try {
            this.store.save();
        } catch (IOException e) {
            LOGGER.error("Failed to save {}", this.name, e);
        }
    }

    /// Stores the tokens in a single vault write, so either all of them are saved or none are.
    public void storeTokens(I id, Map<K, String> tokens) throws VaultException {
        Map<String, String> entries = new HashMap<>();
        tokens.forEach((key, value) -> entries.put(this.tokenKey(id, key), value));
        this.vault.putAll(entries);
    }

    /// @return the token, or `null` if it's missing or the vault can't be read
    @Nullable
    public String token(I id, K key) {
        try {
            return this.vault.get(this.tokenKey(id, key));
        } catch (VaultException e) {
            LOGGER.error("Failed to read {} for {} account {} from the vault", key.getKeyName(), this.name, id, e);
            return null;
        }
    }

    private void removeTokens(I id) {
        try {
            for (K key : EnumSet.allOf(this.tokenKeyType)) {
                this.vault.remove(this.tokenKey(id, key));
            }
        } catch (VaultException e) {
            LOGGER.error("Failed to remove tokens for {} account {} from the vault", this.name, id, e);
        }
    }

    private String tokenKey(I id, K key) {
        return this.vaultPrefix + "." + id + "." + key.getKeyName();
    }

    @Nullable
    private T find(Accounts<T, I> data, I id) {
        for (T profile : data.profiles) {
            if (id.equals(this.idOf.apply(profile))) {
                return profile;
            }
        }
        return null;
    }

    private JsonElement migrator(JsonElement jsonElement) {
        // this shouldn't happen but we'll support it anyway
        if (!jsonElement.isJsonObject()) return jsonElement;

        var obj = jsonElement.getAsJsonObject();
        if (obj.has("version")) {
            // Nothing to migrate.
            String currentVersion = obj.get("version").getAsString();
            if (currentVersion.equals(this.specVersion)) {
                return jsonElement;
            }

            return runMigrator(obj, currentVersion);
        }

        // Every file this class has written has a version, so treat a missing one as current
        return jsonElement;
    }

    /// Upgrades a file written with an older `version`. Runs on the raw JSON before Gson reads it, during [#load()].
    /// Should set `version` to the current spec version once migrated. Returns the input unchanged by default.
    protected JsonObject runMigrator(JsonObject obj, String currentVersion) {
        return obj;
    }
}
