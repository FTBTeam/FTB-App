package net.creeperhost.creeperlauncher.migration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A Migrator, capable of migrating between 2 distinct Data Format versions.
 * <p>
 * A Migrator should be able to properly handle execution failing, or stopping half way through
 * and resuming its operations. Any internal state required by other Migrators should be re-setup
 * when execution resumes. However, it is acceptable to yeet the data and start from scratch.
 * <p>
 * You are required to have a {@link Properties} annotation on your Migrator implementation,
 * in order to specify what Data format you read from and write to. You can also declare any
 * other Migrators you depend on.
 * <p>
 * Created by covers1624 on 13/1/21.
 */
public interface Migrator {

    /**
     * Operates on the current context.
     * Your {@link Properties#requires()} have already been run
     * as well as any prior migrators.
     *
     * @param ctx The context to operate on.
     */
    void operate(MigrationContext ctx) throws MigrationException;

    /**
     * Defines properties for a Migrator.
     */
    @Target (ElementType.TYPE)
    @Retention (RetentionPolicy.RUNTIME)
    @interface Properties {

        /**
         * @return The Data Format version this Handler is capable of migrating from.
         */
        int from();

        /**
         * @return The Data Format version this Handler Migrates to.
         */
        int to();

        /**
         * Specifies which Migrators this one must run after.
         *
         * @return The other migrators.
         */
        Class<? extends Migrator>[] requires() default {};
    }

    /**
     * Thrown when a fatal error occurred whilst executing a migrator.
     */
    class MigrationException extends Exception {

        public MigrationException(String message) {
            super(message);
        }

        public MigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
