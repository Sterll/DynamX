package fr.dynamx.common.contentpack.mps;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.utils.errors.DynamXErrorManager;

/**
 * Centralises MPS-related error reporting so that the protection pipeline does
 * not depend directly on {@link DynamXErrorManager}'s {@code mps_error} formatter
 * (which used to be registered by {@code fr.aym.mps.ModProtectionSystem}).
 *
 * <p>Each helper routes the error through {@link DynamXErrorManager#INIT_ERRORS}
 * under the generic type {@code mps_error}, matching the formatter wiring done
 * statically at the bottom of {@link DynamXErrorManager}.
 *
 * TODO port:1.20.1 - When ModProtectionLib is ported, the dedicated
 *  {@code ModProtectionSystem.getMpsErrorCategory()} should replace
 *  {@link DynamXErrorManager#INIT_ERRORS} so MPS errors show in their own bucket.
 */
public final class DynamXMpsErrors {

    private static final String GENERIC_TYPE = "mps_error";

    private DynamXMpsErrors() {
    }

    public static void reportFatal(String pack, String object, String message, Exception cause) {
        DynamXErrorManager.addError(pack, DynamXErrorManager.INIT_ERRORS, GENERIC_TYPE, ErrorLevel.FATAL, object, message, cause, 750);
    }

    public static void reportFatal(String pack, String object, String message) {
        DynamXErrorManager.addError(pack, DynamXErrorManager.INIT_ERRORS, GENERIC_TYPE, ErrorLevel.FATAL, object, message);
    }

    public static void reportHigh(String pack, String object, String message, Exception cause) {
        DynamXErrorManager.addError(pack, DynamXErrorManager.INIT_ERRORS, GENERIC_TYPE, ErrorLevel.HIGH, object, message, cause, 700);
    }
}
