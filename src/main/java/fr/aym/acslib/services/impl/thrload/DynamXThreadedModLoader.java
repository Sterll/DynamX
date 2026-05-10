// TODO port:1.20.1 stub - DynamXThreadedModLoader runs tasks synchronously
package fr.aym.acslib.services.impl.thrload;

import fr.aym.acslib.api.services.ThreadedLoadingService;

public class DynamXThreadedModLoader implements ThreadedLoadingService {
    @Override
    public void addTask(ModLoadingSteps step, String name, Runnable task, Runnable followingInThreadTask) {
        if (task != null) task.run();
        if (followingInThreadTask != null) followingInThreadTask.run();
    }

    @Override
    public String getName() { return "ThrLoad"; }

    @Override
    public String getVersion() { return "0.0.0-stub"; }
}
