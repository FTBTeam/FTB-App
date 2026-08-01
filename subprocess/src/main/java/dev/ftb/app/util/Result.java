package dev.ftb.app.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A container for a result that can be either a success ({@link Ok}) or an error ({@link Err}).
 *
 * @param <TResult> the resulting datatype
 * @param <TError> the resulting error
 */
public abstract sealed class Result<TResult, TError> {
    private Result() {}

    /**
     * Creates a new {@link Ok} result with the given value.
     */
    public static <TResult, TError> Result<TResult, TError> ok(TResult value) {
        return new Ok<>(value);
    }

    /**
     * Creates a new {@link Err} result with the given error.
     */
    public static <TResult, TError> Result<TResult, TError> err(TError error) {
        return new Err<>(error);
    }

    public boolean isOk() {
        return this instanceof Ok<TResult, TError>;
    }

    public boolean isErr() {
        return this instanceof Err<TResult, TError>;
    }

    public TResult unwrap() {
        return switch (this) {
            case Ok<TResult, TError> ok -> ok.value();
            case Err<TResult, TError> err -> throw new IllegalStateException("Cannot unwrap value from an error result: " + err.error());
        };
    }

    public TError unwrapErr() {
        return switch (this) {
            case Err<TResult, TError> err -> err.error();
            case Ok<TResult, TError> ok -> throw new IllegalStateException("Cannot unwrap error from a success result: " + ok.value());
        };
    }

    public TResult unwrapOr(TResult fallback) {
        return switch (this) {
            case Ok<TResult, TError> ok -> ok.value();
            case Err<TResult, TError> _ -> fallback;
        };
    }

    public TResult unwrapOrElse(Function<TError, TResult> elseFn) {
        return switch (this) {
            case Ok<TResult, TError> ok -> ok.value();
            case Err<TResult, TError> err -> elseFn.apply(err.error());
        };
    }

    /**
     * Transforms the success value, passing errors through unchanged.
     */
    public <TMapped> Result<TMapped, TError> map(Function<TResult, TMapped> mapper) {
        return switch (this) {
            case Ok<TResult, TError> ok -> Result.ok(mapper.apply(ok.value()));
            case Err<TResult, TError> err -> Result.err(err.error());
        };
    }

    /**
     * Transforms the error value, passing successes through unchanged.
     */
    public <TMappedError> Result<TResult, TMappedError> mapErr(Function<TError, TMappedError> mapper) {
        return switch (this) {
            case Ok<TResult, TError> ok -> Result.ok(ok.value());
            case Err<TResult, TError> err -> Result.err(mapper.apply(err.error()));
        };
    }

    /**
     * Chains another fallible step onto a success, stopping on errors.
     */
    public <TMapped> Result<TMapped, TError> andThen(Function<TResult, Result<TMapped, TError>> mapper) {
        return switch (this) {
            case Ok<TResult, TError> ok -> mapper.apply(ok.value());
            case Err<TResult, TError> err -> Result.err(err.error());
        };
    }

    public Result<TResult, TError> ifOk(Consumer<TResult> consumer) {
        if (this instanceof Ok<TResult, TError> ok) {
            consumer.accept(ok.value());
        }

        return this;
    }

    public Result<TResult, TError> ifErr(Consumer<TError> consumer) {
        if (this instanceof Err<TResult, TError> err) {
            consumer.accept(err.error());
        }

        return this;
    }

    /**
     * A result representing a successful computation.
     * @param <TResult>
     * @param <TError>
     */
    public static final class Ok<TResult, TError> extends Result<TResult, TError> {
        private final TResult value;

        private Ok(TResult value) {
            this.value = value;
        }

        public TResult value() {
            return value;
        }

        @Override
        public String toString() {
            return "Ok(" + value + ")";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Ok<?, ?> other && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }

    /**
     * A result representing a failed computation.
     * 
     * @param <TResult>
     * @param <TError>
     */
    public static final class Err<TResult, TError> extends Result<TResult, TError> {
        private final TError error;

        private Err(TError error) {
            this.error = error;
        }

        public TError error() {
            return error;
        }

        @Override
        public String toString() {
            return "Err(" + error + ")";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Err<?, ?> other && Objects.equals(error, other.error);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(error);
        }
    }
}
