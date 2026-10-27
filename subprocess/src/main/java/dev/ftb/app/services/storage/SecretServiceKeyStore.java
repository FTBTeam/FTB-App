package dev.ftb.app.services.storage;

import net.covers1624.quack.platform.OperatingSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static java.lang.foreign.ValueLayout.*;

/// Stores the master key in the freedesktop Secret Service (GNOME Keyring, KWallet 5.97+, KeePassXC) via libsecret.
///
/// Calls can block while the user unlocks their keyring. libsecret is loaded at runtime and is usually present
/// wherever a desktop keyring is, since Chromium loads it the same way.
public final class SecretServiceKeyStore implements MasterKeyStore {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SCHEMA_NAME = "dev.ftb.app.Vault";
    private static final String ATTRIBUTE = "key";
    private static final String ATTRIBUTE_VALUE = "master-key";
    private static final String LABEL = "FTB App Vault";

    private static final int SECRET_SCHEMA_NONE = 0;
    private static final int SECRET_SCHEMA_ATTRIBUTE_STRING = 0;

    /// ```c
    /// typedef struct {
    ///     const gchar *name;
    ///     SecretSchemaFlags flags;
    ///     SecretSchemaAttribute attributes[32]; // { const gchar *name; SecretSchemaAttributeType type; }
    ///     gint reserved;
    ///     gpointer reserved1 ... reserved7;
    /// } SecretSchema;
    /// ```
    private static final StructLayout SCHEMA_ATTRIBUTE = MemoryLayout.structLayout(
            ADDRESS.withName("name"),
            JAVA_INT.withName("type"),
            MemoryLayout.paddingLayout(4)
    );
    
    private static final StructLayout SECRET_SCHEMA = MemoryLayout.structLayout(
            ADDRESS.withName("name"),
            JAVA_INT.withName("flags"),
            MemoryLayout.paddingLayout(4),
            MemoryLayout.sequenceLayout(32, SCHEMA_ATTRIBUTE).withName("attributes"),
            JAVA_INT.withName("reserved"),
            MemoryLayout.paddingLayout(4),
            MemoryLayout.sequenceLayout(7, ADDRESS).withName("reservedPointers")
    );
    private static final long ATTRIBUTES_OFFSET = SECRET_SCHEMA.byteOffset(MemoryLayout.PathElement.groupElement("attributes"));

    /// `struct GError { GQuark domain; gint code; gchar *message; }`
    private static final StructLayout G_ERROR = MemoryLayout.structLayout(
            JAVA_INT.withName("domain"),
            JAVA_INT.withName("code"),
            ADDRESS.withName("message")
    );
    private static final long G_ERROR_MESSAGE_OFFSET = G_ERROR.byteOffset(MemoryLayout.PathElement.groupElement("message"));

    private @Nullable Native nativeFns;

    @Override
    public byte @Nullable [] load() throws VaultException {
        Native fns = natives();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(ADDRESS);
            MemorySegment password = (MemorySegment) fns.lookup.invokeExact(
                    fns.schema, MemorySegment.NULL, error,
                    arena.allocateFrom(ATTRIBUTE), arena.allocateFrom(ATTRIBUTE_VALUE), MemorySegment.NULL
            );
            checkError(fns, error, "secret_password_lookup_sync");
            if (password.equals(MemorySegment.NULL)) return null;

            try {
                return Base64.getDecoder().decode(password.reinterpret(Long.MAX_VALUE).getString(0));
            } finally {
                fns.passwordFree.invokeExact(password);
            }
        } catch (VaultException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new VaultException("Failed to read master key from the Secret Service.", ex);
        }
    }

    @Override
    public void save(byte[] key) throws VaultException {
        Native fns = natives();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(ADDRESS);
            // A null collection means the default collection, usually the "login" keyring
            int ok = (int) fns.store.invokeExact(
                    fns.schema, MemorySegment.NULL, arena.allocateFrom(LABEL), arena.allocateFrom(Base64.getEncoder().encodeToString(key)),
                    MemorySegment.NULL, error,
                    arena.allocateFrom(ATTRIBUTE), arena.allocateFrom(ATTRIBUTE_VALUE), MemorySegment.NULL
            );
            checkError(fns, error, "secret_password_store_sync");
            if (ok == 0) {
                throw new VaultException("secret_password_store_sync failed.");
            }
        } catch (VaultException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new VaultException("Failed to write master key to the Secret Service.", ex);
        }
    }

    @Override
    public void delete() throws VaultException {
        Native fns = natives();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(ADDRESS);
            // Returns false when nothing matched, which is fine
            int ignored = (int) fns.clear.invokeExact(
                    fns.schema, MemorySegment.NULL, error,
                    arena.allocateFrom(ATTRIBUTE), arena.allocateFrom(ATTRIBUTE_VALUE), MemorySegment.NULL
            );
            checkError(fns, error, "secret_password_clear_sync");
        } catch (VaultException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new VaultException("Failed to delete master key from the Secret Service.", ex);
        }
    }

    @Override
    public boolean isAvailable() {
        if (OperatingSystem.current().isWindows() || OperatingSystem.current().isMacos()) return false;
        if (!hasSessionBus()) {
            LOGGER.info("No D-Bus session bus, the Secret Service is unavailable.");
            return false;
        }

        try {
            natives();
            return true;
        } catch (Throwable ex) {
            LOGGER.warn("libsecret is unavailable.", ex);
            return false;
        }
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String id() {
        return "secret-service";
    }

    /// GDBus uses the env var, or falls back to the standard socket in the runtime dir.
    private static boolean hasSessionBus() {
        if (System.getenv("DBUS_SESSION_BUS_ADDRESS") != null) return true;

        String runtimeDir = System.getenv("XDG_RUNTIME_DIR");
        return runtimeDir != null && Files.exists(Path.of(runtimeDir, "bus"));
    }

    private static void checkError(Native fns, MemorySegment errorPtr, String call) throws Throwable {
        MemorySegment error = errorPtr.get(ADDRESS, 0);
        if (error.equals(MemorySegment.NULL)) return;

        error = error.reinterpret(G_ERROR.byteSize());
        String message = error.get(ADDRESS, G_ERROR_MESSAGE_OFFSET).reinterpret(Long.MAX_VALUE).getString(0);
        fns.errorFree.invokeExact(error);
        throw new VaultException(call + " failed: " + message);
    }

    private synchronized Native natives() throws VaultException {
        if (nativeFns == null) {
            try {
                nativeFns = new Native();
            } catch (Throwable ex) {
                throw new VaultException("Failed to load libsecret.", ex);
            }
        }
        return nativeFns;
    }

    /// Generated by Claude as it actually understands the underlying C API and I definitely don't.
    private static final class Native {
        final MethodHandle store;
        final MethodHandle lookup;
        final MethodHandle clear;
        final MethodHandle passwordFree;
        final MethodHandle errorFree;

        /// Lives for the life of the app, libsecret keeps a reference to the schema.
        final MemorySegment schema;

        Native() {
            Linker linker = Linker.nativeLinker();
            SymbolLookup secret = SymbolLookup.libraryLookup("libsecret-1.so.0", Arena.global());
            SymbolLookup glib = SymbolLookup.libraryLookup("libglib-2.0.so.0", Arena.global());

            // All three are variadic, taking NULL terminated attribute name/value pairs. We pass one pair.
            // gboolean secret_password_store_sync(schema, collection, label, password, cancellable, error, ...)
            store = linker.downcallHandle(secret.findOrThrow("secret_password_store_sync"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS),
                    Linker.Option.firstVariadicArg(6));
            // gchar *secret_password_lookup_sync(schema, cancellable, error, ...)
            lookup = linker.downcallHandle(secret.findOrThrow("secret_password_lookup_sync"),
                    FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS),
                    Linker.Option.firstVariadicArg(3));
            // gboolean secret_password_clear_sync(schema, cancellable, error, ...)
            clear = linker.downcallHandle(secret.findOrThrow("secret_password_clear_sync"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS),
                    Linker.Option.firstVariadicArg(3));
            // Wipes the memory before freeing it
            passwordFree = linker.downcallHandle(secret.findOrThrow("secret_password_free"), FunctionDescriptor.ofVoid(ADDRESS));
            errorFree = linker.downcallHandle(glib.findOrThrow("g_error_free"), FunctionDescriptor.ofVoid(ADDRESS));

            Arena global = Arena.global();
            schema = global.allocate(SECRET_SCHEMA);
            schema.set(ADDRESS, 0, global.allocateFrom(SCHEMA_NAME));
            schema.set(JAVA_INT, ADDRESS.byteSize(), SECRET_SCHEMA_NONE);
            schema.set(ADDRESS, ATTRIBUTES_OFFSET, global.allocateFrom(ATTRIBUTE));
            schema.set(JAVA_INT, ATTRIBUTES_OFFSET + ADDRESS.byteSize(), SECRET_SCHEMA_ATTRIBUTE_STRING);
            // The remaining attributes stay zeroed, a null name terminates the list
        }
    }
}
