package net.creeperhost.creeperlauncher.util;

import java.util.function.Supplier;

/**
 * To the best of my ability, a 1:1 implementation of the rust like Result class / impl. It's not 100%, I might come back 
 * and improve it if I find a need for some of the more complex methods
 * 
 * @param <TResult> the resulting datatype
 * @param <TError> the resulting error
 *                
 * @implNote Specifically not used @Nullable as that's the intention of this class. We avoid having to do null checks 
 */
public final class Result<TResult, TError> {
    private final TResult value;
    private final TError error;

    private Result(TResult value, TError error) {
        this.value = value;
        this.error = error;
    }

    public static <TResult, TError> Result<TResult, TError> ok(TResult value) {
        return new Result<>(value, null);
    }

    public static <TResult, TError> Result<TResult, TError> err(TError error) {
        return new Result<>(null, error);
    }

    public boolean isOk() {
        return value != null;
    }

    public boolean isErr() {
        return error != null;
    }

    public TResult unwrap() {
        if (isOk()) {
            return value;
        }

        throw new IllegalStateException("Cannot unwrap value from an error result.");
    }

    public TError unwrapErr() {
        if (isErr()) {
            return this.error;
        }

        throw new IllegalStateException("Cannot unwrap error from a success result.");
    }

    public TResult unwrapOrElse(Supplier<TResult> elseFn) {
        return isErr() ? elseFn.get() : value;
    }
}
