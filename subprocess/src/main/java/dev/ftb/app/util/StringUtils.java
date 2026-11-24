package dev.ftb.app.util;

import org.jspecify.annotations.Nullable;

public class StringUtils {
    public static boolean isBlank(@Nullable String str) {
        return str == null || str.isBlank();
    }
}
