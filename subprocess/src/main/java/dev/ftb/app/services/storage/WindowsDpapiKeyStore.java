package dev.ftb.app.services.storage;

import dev.ftb.app.util.SafeFiles;
import net.covers1624.quack.platform.OperatingSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.foreign.ValueLayout.*;

/// Protects the master key with DPAPI (`CryptProtectData`), the same approach Chromium uses on Windows.
///
/// The protected blob is written to a file next to the vault. Only the same Windows user on the same machine can
/// unprotect it. If an admin resets the user's password (rather than the user changing it), DPAPI can no longer
/// decrypt the blob and the vault has to be reset.
public final class WindowsDpapiKeyStore implements MasterKeyStore {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int CRYPTPROTECT_UI_FORBIDDEN = 0x1;
    /// Ties blobs to this app, so a blob protected by something else can't be passed off as ours.
    private static final byte[] ENTROPY = "dev.ftb.app.vault".getBytes(StandardCharsets.UTF_8);

    /// `typedef struct { DWORD cbData; BYTE *pbData; } DATA_BLOB;`
    private static final StructLayout DATA_BLOB = MemoryLayout.structLayout(
            JAVA_INT.withName("cbData"),
            MemoryLayout.paddingLayout(4),
            ADDRESS.withName("pbData")
    );
    private static final VarHandle CB_DATA = DATA_BLOB.varHandle(MemoryLayout.PathElement.groupElement("cbData"));
    private static final VarHandle PB_DATA = DATA_BLOB.varHandle(MemoryLayout.PathElement.groupElement("pbData"));

    private final Path file;
    private @Nullable Native nativeFns;

    public WindowsDpapiKeyStore(Path file) {
        this.file = file;
    }

    @Override
    public byte @Nullable [] load() throws VaultException {
        if (!Files.exists(file)) return null;

        try {
            return call(Files.readAllBytes(file), false);
        } catch (IOException ex) {
            throw new VaultException("Failed to read DPAPI key file.", ex);
        }
    }

    @Override
    public void save(byte[] key) throws VaultException {
        try {
            SafeFiles.writeAtomically(file, call(key, true));
        } catch (IOException ex) {
            throw new VaultException("Failed to write DPAPI key file.", ex);
        }
    }

    @Override
    public void delete() throws VaultException {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new VaultException("Failed to delete DPAPI key file.", ex);
        }
    }

    @Override
    public boolean isAvailable() {
        if (!OperatingSystem.current().isWindows()) return false;
        try {
            natives();
            return true;
        } catch (Throwable ex) {
            LOGGER.warn("DPAPI is unavailable.", ex);
            return false;
        }
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String id() {
        return "dpapi";
    }

    private byte[] call(byte[] input, boolean protect) throws VaultException {
        Native fns = natives();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment in = blob(arena, input);
            MemorySegment entropy = blob(arena, ENTROPY);
            MemorySegment out = arena.allocate(DATA_BLOB);
            MemorySegment callState = arena.allocate(Native.CALL_STATE);

            MethodHandle fn = protect ? fns.cryptProtectData : fns.cryptUnprotectData;
            int ok = (int) fn.invokeExact(callState, in, MemorySegment.NULL, entropy, MemorySegment.NULL, MemorySegment.NULL, CRYPTPROTECT_UI_FORBIDDEN, out);
            if (ok == 0) {
                int error = (int) Native.LAST_ERROR.get(callState, 0L);
                throw new VaultException((protect ? "CryptProtectData" : "CryptUnprotectData") + " failed with error " + error + ".");
            }

            int size = (int) CB_DATA.get(out, 0L);
            MemorySegment data = (MemorySegment) PB_DATA.get(out, 0L);
            try {
                return data.reinterpret(size).toArray(JAVA_BYTE);
            } finally {
                MemorySegment ignored = (MemorySegment) fns.localFree.invokeExact(data);
            }
        } catch (VaultException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new VaultException("DPAPI call failed.", ex);
        }
    }

    private static MemorySegment blob(Arena arena, byte[] bytes) {
        MemorySegment blob = arena.allocate(DATA_BLOB);
        CB_DATA.set(blob, 0L, bytes.length);
        PB_DATA.set(blob, 0L, arena.allocateFrom(JAVA_BYTE, bytes));
        return blob;
    }

    private synchronized Native natives() throws VaultException {
        if (nativeFns == null) {
            try {
                nativeFns = new Native();
            } catch (Throwable ex) {
                throw new VaultException("Failed to load crypt32.", ex);
            }
        }
        return nativeFns;
    }

    /// Generated by Claude as it actually understands the underlying C API and I definitely don't.
    private static final class Native {
        static final StructLayout CALL_STATE = Linker.Option.captureStateLayout();
        static final VarHandle LAST_ERROR = CALL_STATE.varHandle(MemoryLayout.PathElement.groupElement("GetLastError"));

        final MethodHandle cryptProtectData;
        final MethodHandle cryptUnprotectData;
        final MethodHandle localFree;

        Native() {
            Linker linker = Linker.nativeLinker();
            SymbolLookup crypt32 = SymbolLookup.libraryLookup("crypt32", Arena.global());
            SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());
            Linker.Option captureLastError = Linker.Option.captureCallState("GetLastError");

            // Both share a shape: (DATA_BLOB* in, LPCWSTR/LPWSTR* desc, DATA_BLOB* entropy, PVOID reserved,
            //                      CRYPTPROTECT_PROMPTSTRUCT* prompt, DWORD flags, DATA_BLOB* out) -> BOOL
            FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS);
            cryptProtectData = linker.downcallHandle(crypt32.findOrThrow("CryptProtectData"), descriptor, captureLastError);
            cryptUnprotectData = linker.downcallHandle(crypt32.findOrThrow("CryptUnprotectData"), descriptor, captureLastError);
            localFree = linker.downcallHandle(kernel32.findOrThrow("LocalFree"), FunctionDescriptor.of(ADDRESS, ADDRESS));
        }
    }
}
