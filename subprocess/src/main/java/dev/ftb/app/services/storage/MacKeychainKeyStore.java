package dev.ftb.app.services.storage;

import net.covers1624.quack.platform.OperatingSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.ValueLayout.*;

/// Stores the master key as a generic password in the user's login keychain, using the `SecItem` API.
///
/// The keychain grants access to the binary that created the item, which for us is the bundled `java` runtime. That
/// means a runtime update can trigger a one-time "java wants to access your keychain" prompt, and other processes
/// running the same signed runtime can read the item. It still protects the key at rest, which is the goal.
public final class MacKeychainKeyStore implements MasterKeyStore {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SERVICE = "dev.ftb.app";
    private static final String ACCOUNT = "vault-master-key";
    private static final String LABEL = "FTB App Vault";

    private static final int ERR_SEC_SUCCESS = 0;
    private static final int ERR_SEC_ITEM_NOT_FOUND = -25300;

    private @Nullable Native nativeFns;

    @Override
    public byte @Nullable [] load() throws VaultException {
        Native fns = natives();
        try (CfScope cf = new CfScope(fns); Arena arena = Arena.ofConfined()) {
            Map<MemorySegment, MemorySegment> query = baseQuery(cf);
            query.put(fns.kSecReturnData, fns.kCFBooleanTrue);
            query.put(fns.kSecMatchLimit, fns.kSecMatchLimitOne);

            MemorySegment result = arena.allocate(ADDRESS);
            int status = (int) fns.secItemCopyMatching.invokeExact(cf.dictionary(query), result);
            if (status == ERR_SEC_ITEM_NOT_FOUND) return null;
            check(status, "SecItemCopyMatching");

            MemorySegment data = cf.own(result.get(ADDRESS, 0));
            long length = (long) fns.cfDataGetLength.invokeExact(data);
            MemorySegment bytes = (MemorySegment) fns.cfDataGetBytePtr.invokeExact(data);
            return bytes.reinterpret(length).toArray(JAVA_BYTE);
        } catch (VaultException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new VaultException("Failed to read master key from the keychain.", ex);
        }
    }

    @Override
    public void save(byte[] key) throws VaultException {
        Native fns = natives();
        try (CfScope cf = new CfScope(fns)) {
            MemorySegment value = cf.data(key);

            int status = (int) fns.secItemUpdate.invokeExact(cf.dictionary(baseQuery(cf)), cf.dictionary(Map.of(fns.kSecValueData, value)));
            if (status == ERR_SEC_ITEM_NOT_FOUND) {
                Map<MemorySegment, MemorySegment> attributes = baseQuery(cf);
                attributes.put(fns.kSecAttrLabel, cf.string(LABEL));
                attributes.put(fns.kSecValueData, value);
                status = (int) fns.secItemAdd.invokeExact(cf.dictionary(attributes), MemorySegment.NULL);
                check(status, "SecItemAdd");
            } else {
                check(status, "SecItemUpdate");
            }
        } catch (VaultException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new VaultException("Failed to write master key to the keychain.", ex);
        }
    }

    @Override
    public void delete() throws VaultException {
        Native fns = natives();
        try (CfScope cf = new CfScope(fns)) {
            int status = (int) fns.secItemDelete.invokeExact(cf.dictionary(baseQuery(cf)));
            if (status != ERR_SEC_ITEM_NOT_FOUND) {
                check(status, "SecItemDelete");
            }
        } catch (VaultException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new VaultException("Failed to delete master key from the keychain.", ex);
        }
    }

    @Override
    public boolean isAvailable() {
        if (!OperatingSystem.current().isMacos()) return false;
        try {
            natives();
            return true;
        } catch (Throwable ex) {
            LOGGER.warn("Keychain is unavailable.", ex);
            return false;
        }
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String id() {
        return "keychain";
    }

    private Map<MemorySegment, MemorySegment> baseQuery(CfScope cf) throws Throwable {
        Native fns = cf.fns;
        Map<MemorySegment, MemorySegment> query = new LinkedHashMap<>();
        query.put(fns.kSecClass, fns.kSecClassGenericPassword);
        query.put(fns.kSecAttrService, cf.string(SERVICE));
        query.put(fns.kSecAttrAccount, cf.string(ACCOUNT));
        return query;
    }

    private static void check(int status, String call) throws VaultException {
        if (status != ERR_SEC_SUCCESS) {
            // -128 user cancelled, -25293 auth failed, -25308 interaction not allowed
            throw new VaultException(call + " failed with OSStatus " + status + ".");
        }
    }

    private synchronized Native natives() throws VaultException {
        if (nativeFns == null) {
            try {
                nativeFns = new Native();
            } catch (Throwable ex) {
                throw new VaultException("Failed to load Security.framework.", ex);
            }
        }
        return nativeFns;
    }

    /// Creates CoreFoundation objects and releases them all on close.
    private static final class CfScope implements AutoCloseable {
        private static final int K_CF_STRING_ENCODING_UTF8 = 0x08000100;

        final Native fns;
        private final Arena arena = Arena.ofConfined();
        private final List<MemorySegment> owned = new ArrayList<>();

        CfScope(Native fns) {
            this.fns = fns;
        }

        MemorySegment own(MemorySegment ref) {
            if (!ref.equals(MemorySegment.NULL)) {
                owned.add(ref);
            }
            return ref;
        }

        MemorySegment string(String value) throws Throwable {
            return own((MemorySegment) fns.cfStringCreateWithCString.invokeExact(MemorySegment.NULL, arena.allocateFrom(value), K_CF_STRING_ENCODING_UTF8));
        }

        MemorySegment data(byte[] value) throws Throwable {
            return own((MemorySegment) fns.cfDataCreate.invokeExact(MemorySegment.NULL, arena.allocateFrom(JAVA_BYTE, value), (long) value.length));
        }

        MemorySegment dictionary(Map<MemorySegment, MemorySegment> entries) throws Throwable {
            MemorySegment keys = arena.allocate(ADDRESS, entries.size());
            MemorySegment values = arena.allocate(ADDRESS, entries.size());
            int i = 0;
            for (Map.Entry<MemorySegment, MemorySegment> entry : entries.entrySet()) {
                keys.setAtIndex(ADDRESS, i, entry.getKey());
                values.setAtIndex(ADDRESS, i, entry.getValue());
                i++;
            }
            return own((MemorySegment) fns.cfDictionaryCreate.invokeExact(MemorySegment.NULL, keys, values, (long) entries.size(), fns.kCFTypeDictionaryKeyCallBacks, fns.kCFTypeDictionaryValueCallBacks));
        }

        @Override
        public void close() {
            for (MemorySegment ref : owned.reversed()) {
                try {
                    fns.cfRelease.invokeExact(ref);
                } catch (Throwable ex) {
                    LOGGER.warn("Failed to release CoreFoundation object.", ex);
                }
            }
            arena.close();
        }
    }

    /// Generated by Claude as it actually understands the underlying C API and I definitely don't.
    private static final class Native {
        final MethodHandle cfStringCreateWithCString;
        final MethodHandle cfDataCreate;
        final MethodHandle cfDataGetLength;
        final MethodHandle cfDataGetBytePtr;
        final MethodHandle cfDictionaryCreate;
        final MethodHandle cfRelease;

        final MethodHandle secItemCopyMatching;
        final MethodHandle secItemAdd;
        final MethodHandle secItemUpdate;
        final MethodHandle secItemDelete;

        final MemorySegment kCFBooleanTrue;
        final MemorySegment kCFTypeDictionaryKeyCallBacks;
        final MemorySegment kCFTypeDictionaryValueCallBacks;

        final MemorySegment kSecClass;
        final MemorySegment kSecClassGenericPassword;
        final MemorySegment kSecAttrService;
        final MemorySegment kSecAttrAccount;
        final MemorySegment kSecAttrLabel;
        final MemorySegment kSecValueData;
        final MemorySegment kSecReturnData;
        final MemorySegment kSecMatchLimit;
        final MemorySegment kSecMatchLimitOne;

        Native() {
            Linker linker = Linker.nativeLinker();
            SymbolLookup cf = SymbolLookup.libraryLookup("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation", Arena.global());
            SymbolLookup sec = SymbolLookup.libraryLookup("/System/Library/Frameworks/Security.framework/Security", Arena.global());

            cfStringCreateWithCString = linker.downcallHandle(cf.findOrThrow("CFStringCreateWithCString"), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT));
            cfDataCreate = linker.downcallHandle(cf.findOrThrow("CFDataCreate"), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));
            cfDataGetLength = linker.downcallHandle(cf.findOrThrow("CFDataGetLength"), FunctionDescriptor.of(JAVA_LONG, ADDRESS));
            cfDataGetBytePtr = linker.downcallHandle(cf.findOrThrow("CFDataGetBytePtr"), FunctionDescriptor.of(ADDRESS, ADDRESS));
            cfDictionaryCreate = linker.downcallHandle(cf.findOrThrow("CFDictionaryCreate"), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS));
            cfRelease = linker.downcallHandle(cf.findOrThrow("CFRelease"), FunctionDescriptor.ofVoid(ADDRESS));

            secItemCopyMatching = linker.downcallHandle(sec.findOrThrow("SecItemCopyMatching"), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            secItemAdd = linker.downcallHandle(sec.findOrThrow("SecItemAdd"), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            secItemUpdate = linker.downcallHandle(sec.findOrThrow("SecItemUpdate"), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            secItemDelete = linker.downcallHandle(sec.findOrThrow("SecItemDelete"), FunctionDescriptor.of(JAVA_INT, ADDRESS));

            kCFBooleanTrue = constant(cf, "kCFBooleanTrue");
            // These two are structs, the API wants their address rather than their value
            kCFTypeDictionaryKeyCallBacks = cf.findOrThrow("kCFTypeDictionaryKeyCallBacks");
            kCFTypeDictionaryValueCallBacks = cf.findOrThrow("kCFTypeDictionaryValueCallBacks");

            kSecClass = constant(sec, "kSecClass");
            kSecClassGenericPassword = constant(sec, "kSecClassGenericPassword");
            kSecAttrService = constant(sec, "kSecAttrService");
            kSecAttrAccount = constant(sec, "kSecAttrAccount");
            kSecAttrLabel = constant(sec, "kSecAttrLabel");
            kSecValueData = constant(sec, "kSecValueData");
            kSecReturnData = constant(sec, "kSecReturnData");
            kSecMatchLimit = constant(sec, "kSecMatchLimit");
            kSecMatchLimitOne = constant(sec, "kSecMatchLimitOne");
        }

        /// The `kSec*`/`kCF*` constants are global pointer variables, read the pointer they hold.
        private static MemorySegment constant(SymbolLookup lookup, String name) {
            return lookup.findOrThrow(name).reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
        }
    }
}
