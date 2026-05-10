// TODO port:1.20.1 stub - factory creating stub service instances
package fr.aym.acslib.impl;

import fr.aym.acslib.api.ACsService;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.aym.acslib.api.services.error.ErrorManagerService;
import fr.aym.acsguis.api.ACsGuiApiService;

public class StubServiceFactory {
    public static Object create(Class<?> serviceClass) {
        if (serviceClass == ErrorManagerService.class) {
            return new ErrorManagerService();
        }
        if (serviceClass == ThreadedLoadingService.class) {
            return new fr.aym.acslib.services.impl.thrload.DynamXThreadedModLoader();
        }
        if (serviceClass == ACsGuiApiService.class) {
            return new ACsGuiApiService();
        }
        return null;
    }
}
