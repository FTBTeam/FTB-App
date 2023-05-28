package net.creeperhost.creeperlauncher.util;

import com.google.common.base.Objects;

import java.util.Optional;

/**
 * Holds either a Data object for a valid response or an Error object for a failed response
 *
 * @param <D> data
 * @param <E> error
 *           
 * @deprecated use {@link Result} instead!  
 */
@Deprecated
public abstract class DataResult<D, E> {
    public abstract Optional<D> data();

    public abstract Optional<E> error();

    public static <D, E> DataResult<D, E> data(D value) {
        return new Data<>(value);
    }

    public static <D, E> DataResult<D, E> error(E value) {
        return new Error<>(value);
    }

    /**
     * Inner handler of a valid error with invalid data
     */
    private static final class Error<D, E> extends DataResult<D, E> {
        private final E value;

        private Error(E value) {
            this.value = value;
        }

        @Override
        public Optional<D> data() {
            return Optional.empty();
        }

        @Override
        public Optional<E> error() {
            return Optional.of(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Error<?, ?> error = (Error<?, ?>) o;
            return Objects.equal(value, error.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }

    /**
     * Inner handler of a valid data result with an invalid error
     */
    private static final class Data<D, E> extends DataResult<D, E> {
        private final D value;

        private Data(D value) {
            this.value = value;
        }

        @Override
        public Optional<D> data() {
            return Optional.of(value);
        }

        @Override
        public Optional<E> error() {
            return Optional.empty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Data<?, ?> data = (Data<?, ?>) o;
            return Objects.equal(value, data.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }
}
