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

    /**
     * TODO port:1.20.1 - Simple ThreadFactory replacement so the existing call sites compile.
     * Daemon threads named with the supplied prefix.
     */
    public static class DefaultThreadFactory implements java.util.concurrent.ThreadFactory {
        private final String namePrefix;
        private final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);

        public DefaultThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
