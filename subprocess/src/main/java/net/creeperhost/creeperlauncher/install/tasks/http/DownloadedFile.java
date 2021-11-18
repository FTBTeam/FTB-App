package net.creeperhost.creeperlauncher.install.tasks.http;

import com.google.common.hash.HashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public record DownloadedFile(Path destination, long size, @Nullable HashCode checksum) {
}
