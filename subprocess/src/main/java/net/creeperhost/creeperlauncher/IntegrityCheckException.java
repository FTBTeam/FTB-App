package net.creeperhost.creeperlauncher;

import com.google.common.hash.HashCode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class IntegrityCheckException extends RuntimeException
{
    private Throwable otherThrowable = null;
    private int errorCode = 0;
    private HashCode checksum = null;
    private List<HashCode> checksums = new ArrayList<>();
    private long size = 0;
    private long expectedSize = 0;
    private String source = "";
    private Path destination;

    public IntegrityCheckException(String detailMessage)
    {
        super(detailMessage);
    }

    public IntegrityCheckException(Throwable t, int errorCode, HashCode checksum, List<HashCode> checksums, long size, long expectedSize, String source, Path destination)
    {
        this(t.getMessage(), errorCode, checksum, checksums, size, expectedSize, source, destination);
        initCause(t);
        otherThrowable = t;
    }

    public IntegrityCheckException(String detailMessage, int errorCode, HashCode checksum, List<HashCode> checksums, long size, long expectedSize, String source, Path destination)
    {
        super(detailMessage);
        this.errorCode = errorCode;
        this.checksum = checksum;
        this.checksums = checksums;
        this.size = size;
        this.expectedSize = expectedSize;
        this.source = source;
        this.destination = destination;
    }

    @Override
    public String getMessage() {
        StringBuilder errorString = new StringBuilder();
        if (otherThrowable != null)
        {
            errorString.append("Caught throwable: ").append(otherThrowable.getMessage()).append("\n");
        }
        errorString.append("errorCode: ").append(errorCode).append("\n");
        errorString.append("checksum: ").append(checksum).append("\n");
        for (HashCode validChecksum : checksums)
        {
            errorString.append("validChecksum: ").append(validChecksum).append("\n");
        }
        errorString.append("size: ").append(size).append("\n");
        errorString.append("expectedSize: ").append(expectedSize).append("\n");
        errorString.append("source: ").append(source).append("\n");
        errorString.append("destination: ").append(destination == null ? "" : destination.toString()).append("\n");
        return super.getMessage() + "\n" + errorString.toString();
    }
}
