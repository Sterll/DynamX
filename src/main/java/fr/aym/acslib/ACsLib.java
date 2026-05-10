// TODO port:1.20.1 stub - ACsLib platform entry point
package fr.aym.acslib;

import fr.aym.acslib.api.ACsService;
import fr.aym.acslib.api.services.error.ErrorCategory;

public class ACsLib {
    private static final ACsLib INSTANCE = new ACsLib();
    private static final ErrorCategory ACSLIB_ERRORS = new ErrorCategory();

    public static ACsLib getPlatform() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public <T extends ACsService> T provideService(Class<T> serviceClass) {
        // TODO port:1.20.1 stub - return a no-op instance for known services
        return (T) fr.aym.acslib.impl.StubServiceFactory.create(serviceClass);
    }

    public ErrorCategory getACsLibErrorCategory() {
        return ACSLIB_ERRORS;
    }
}
