package dev.ftb.app.accounts.data;

import com.google.gson.JsonElement;
import okhttp3.Response;

import javax.annotation.Nullable;

public record StepReply(
        boolean success,
        int status,
        String message,
        JsonElement data,
        @Nullable Response rawResponse,
        boolean networkError
) {
    
}
