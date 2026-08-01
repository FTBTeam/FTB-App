package dev.ftb.app.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A container for a result that can be either a success ({@link Ok}) or an error ({@link Err}).
 *
 * @param <R> the resulting datatype
 * @param <E> the resulting error
 */
public abstract sealed class Result<R, E> {
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

    /**
     * Folds a stream of results into a single one: {@code Ok} of all the values if every element was
     * {@code Ok}, or the first {@code Err} encountered, short-circuiting before consuming the rest of the stream.
     */
    public static <T, E> Result<List<T>, E> sequence(Stream<Result<T, E>> results) {
        var values = new ArrayList<T>();

        for (var iterator = results.iterator(); iterator.hasNext(); ) {
            switch (iterator.next()) {
                case Ok<T, E> ok -> values.add(ok.value());
                case Err<T, E> err -> {
                    return Result.err(err.error());
                }
            }
        }

        return Result.ok(values);
    }

    public boolean isOk() {
        return this instanceof Ok<R, E>;
    }

    public boolean isErr() {
        return this instanceof Err<R, E>;
    }

    /**
     * The success value, or empty on {@link Err}. Note that this may contain {@code null} if the {@code Ok} value was {@code null}.
     */
    public Optional<R> ok() {
        return switch (this) {
            case Ok<R, E> ok -> Optional.of(ok.value());
            case Err<R, E> _ -> Optional.empty();
        };
    }

    /**
     * The error value, or empty on {@link Ok}. Note that this may contain {@code null} if the {@code Err} value was {@code null}.
     */
    public Optional<E> err() {
        return switch (this) {
            case Ok<R, E> _ -> Optional.empty();
            case Err<R, E> err -> Optional.of(err.error());
        };
    }

    /**
     * A stream of the success value, or an empty stream on {@link Err} where the stream will contain a single element if {@link Ok} and be empty if {@link Err}.
     * Note that this may contain {@code null} if the {@code Ok} value was {@code null}.
     */
    public Stream<R> stream() {
        return switch (this) {
            case Ok<R, E> ok -> Stream.of(ok.value());
            case Err<R, E> _ -> Stream.empty();
        };
    }

    public R unwrap() {
        return switch (this) {
            case Ok<R, E> ok -> ok.value();
            case Err<R, E> err -> throw new IllegalStateException("Cannot unwrap value from an error result: " + err.error());
        };
    }

    public E unwrapErr() {
        return switch (this) {
            case Err<R, E> err -> err.error();
            case Ok<R, E> ok -> throw new IllegalStateException("Cannot unwrap error from a success result: " + ok.value());
        };
    }

    /**
     * Like {@link #unwrap()}, but with a caller-supplied message instead of the generic one.
     */
    public R expect(String message) {
        return switch (this) {
            case Ok<R, E> ok -> ok.value();
            case Err<R, E> err -> throw new IllegalStateException(message + ": " + err.error());
        };
    }

    /**
     * Like {@link #unwrapErr()}, but with a caller-supplied message instead of the generic one.
     */
    public E expectErr(String message) {
        return switch (this) {
            case Err<R, E> err -> err.error();
            case Ok<R, E> ok -> throw new IllegalStateException(message + ": " + ok.value());
        };
    }

    public R unwrapOr(R fallback) {
        return switch (this) {
            case Ok<R, E> ok -> ok.value();
            case Err<R, E> _ -> fallback;
        };
    }

    public R unwrapOrElse(Function<E, R> elseFn) {
        return switch (this) {
            case Ok<R, E> ok -> ok.value();
            case Err<R, E> err -> elseFn.apply(err.error());
        };
    }

    /**
     * Transforms the success value, passing errors through unchanged.
     */
    public <T> Result<T, E> map(Function<R, T> mapper) {
        return switch (this) {
            case Ok<R, E> ok -> Result.ok(mapper.apply(ok.value()));
            case Err<R, E> err -> Result.err(err.error());
        };
    }

    /**
     * Transforms the error value, passing successes through unchanged.
     */
    public <E2> Result<R, E2> mapErr(Function<E, E2> mapper) {
        return switch (this) {
            case Ok<R, E> ok -> Result.ok(ok.value());
            case Err<R, E> err -> Result.err(mapper.apply(err.error()));
        };
    }

    /**
     * Chains another fallible step onto a success, stopping on errors.
     */
    public <T> Result<T, E> andThen(Function<R, Result<T, E>> mapper) {
        return switch (this) {
            case Ok<R, E> ok -> mapper.apply(ok.value());
            case Err<R, E> err -> Result.err(err.error());
        };
    }

    /**
     * Falls back to {@code fallback} on {@link Err}, passing a success through unchanged.
     */
    public <TNewError> Result<R, TNewError> or(Result<R, TNewError> fallback) {
        return switch (this) {
            case Ok<R, E> ok -> Result.ok(ok.value());
            case Err<R, E> _ -> fallback;
        };
    }

    /**
     * Falls back to the result of {@code fallbackFn} on {@link Err}, passing a success through unchanged.
     */
    public <TNewError> Result<R, TNewError> orElse(Function<E, Result<R, TNewError>> fallbackFn) {
        return switch (this) {
            case Ok<R, E> ok -> Result.ok(ok.value());
            case Err<R, E> err -> fallbackFn.apply(err.error());
        };
    }

    public Result<R, E> ifOk(Consumer<R> consumer) {
        if (this instanceof Ok<R, E> ok) {
            consumer.accept(ok.value());
        }

        return this;
    }

    public Result<R, E> ifErr(Consumer<E> consumer) {
        if (this instanceof Err<R, E> err) {
            consumer.accept(err.error());
        }

        return this;
    }

    /**
     * A result representing a successful computation.
     * @param <R>
     * @param <E>
     */
    public static final class Ok<R, E> extends Result<R, E> {
        private final R value;

        private Ok(R value) {
            this.value = value;
        }

        public R value() {
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
     * @param <R>
     * @param <E>
     */
    public static final class Err<R, E> extends Result<R, E> {
        private final E error;

        private Err(E error) {
            this.error = error;
        }

        public E error() {
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
