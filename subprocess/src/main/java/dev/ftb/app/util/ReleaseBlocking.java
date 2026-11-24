package dev.ftb.app.util;

import java.lang.annotation.*;

/// Marks code that must not ship. A release build fails while any of these remain.
/// 
/// Validated as part of the CI, otherwise, this is just decorative. 
@Documented
@Retention(RetentionPolicy.CLASS) // We don't need this at runtime, just during compilation
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER})
public @interface ReleaseBlocking {
    /// Human-readable reason for why this code is release-blocking.
    String reason();
    
    String resolvedBy() default ""; // Optional, who is responsible for resolving this issue
}