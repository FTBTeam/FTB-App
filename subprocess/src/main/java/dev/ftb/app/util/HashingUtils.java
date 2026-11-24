package dev.ftb.app.util;

import org.apache.commons.codec.digest.MurmurHash2;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.IntStream;

public class HashingUtils {
    public static byte[] sha1(byte[] data) throws IllegalStateException {
        return generateSha("SHA-1", data);
    }

    public static byte[] sha256(byte[] data) throws IllegalStateException {
        return generateSha("SHA-256", data);
    }

    public static byte[] sha512(byte[] data) throws IllegalStateException {
        return generateSha("SHA-512", data);
    }
    
    private static byte[] generateSha(String algo, byte[] data) {
        try {
            return MessageDigest.getInstance(algo).digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // every JVM has SHA-256 and basically all other standard sha gen so this is fairly unneeded.
        }
    }
    
    public static long createCurseForgeMurmurHash(Path file) {
        try {
            var fileBytes = Files.readAllBytes(file);
            return createCurseForgeMurmurHash(fileBytes);
        } catch (Exception e) {
            return 0;
        }
    }
    
    public static long createCurseForgeMurmurHash(byte[] fileBytes) {
        try {            
            var fileBytesWithoutWhitespace = IntStream.range(0, fileBytes.length)
                .map(i -> fileBytes[i])
                .filter(b -> b != 9 && b != 10 && b != 13 && b != 32)
                .collect(ByteArrayOutputStream::new, ByteArrayOutputStream::write, (a, b) -> {})
                .toByteArray();
            
            int signedHash = MurmurHash2.hash32(fileBytesWithoutWhitespace, fileBytesWithoutWhitespace.length, 1);
            if (signedHash < 0) {
                return Integer.toUnsignedLong(signedHash);
            }
            
            return signedHash;
        } catch (Exception e) {
            return 0;
        }
    }
}
