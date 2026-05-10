// TODO port:1.20.1 stub - ModProtectionSystem
package fr.aym.mps;

import fr.aym.acslib.api.services.error.ErrorCategory;

public class ModProtectionSystem {
    private static final ErrorCategory MPS_ERRORS = new ErrorCategory();

    public static ErrorCategory getMpsErrorCategory() {
        return MPS_ERRORS;
    }
}
