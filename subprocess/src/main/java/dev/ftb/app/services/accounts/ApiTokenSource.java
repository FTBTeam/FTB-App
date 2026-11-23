package dev.ftb.app.services.accounts;

import org.jspecify.annotations.Nullable;

/// Bearer tokens for an API
public interface ApiTokenSource {
    /// For when no one is signed in
    ApiTokenSource NONE = new ApiTokenSource() {
        @Override
        public @Nullable String token() {
            return null;
        }

        @Override
        public @Nullable String tokenAfterRejection(String rejected) {
            return null;
        }
    };
    
    @Nullable String token();

    /// The API refused `rejected`. Returns a replacement, or `null`
    @Nullable String tokenAfterRejection(String rejected);
}
