package dev.ftb.app.pack;

import java.util.UUID;

/**
 * Thrown when the instance is unable to be launched for a specific reason.
 * <p>
 */
public class InstanceLaunchException extends Exception {

    public InstanceLaunchException(String message) {
        super(message);
    }

    public InstanceLaunchException(String message, Throwable t) {
        super(message, t);
    }

    public static class Abort extends InstanceLaunchException {

        public Abort(String message) {
            super(message);
        }

        public Abort(String message, Throwable t) {
            super(message, t);
        }
    }

    /// The Minecraft account has to sign in again before it can launch
    public static class SignInRequired extends InstanceLaunchException {
        public final UUID profileUuid;

        public SignInRequired(String message, UUID profileUuid) {
            super(message);
            this.profileUuid = profileUuid;
        }
    }

    /// The scan found duplicate mods or invalid scripts. The user can launch anyway, see [Instance#play].
    public static class PotentiallyInvalid extends InstanceLaunchException {
        public PotentiallyInvalid(String message) {
            super(message);
        }
    }
}
