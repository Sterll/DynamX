// TODO port:1.20.1 stub - ThreadedLoadingService interface
package fr.aym.acslib.api.services;

import fr.aym.acslib.api.ACsService;

public interface ThreadedLoadingService extends ACsService {
    enum ModLoadingSteps {
        NOT_INIT, PRE_INIT, INIT, BLOCK_REGISTRY, ITEM_REGISTRY, POST_INIT, FINISH_LOAD;
        public int getIndex() { return ordinal(); }
    }

    default void addTask(ModLoadingSteps step, String name, Runnable task) {
        addTask(step, name, task, null);
    }

    void addTask(ModLoadingSteps step, String name, Runnable task, Runnable followingInThreadTask);

    default void addTask(Runnable task) {
        task.run();
    }

    default boolean mcLoadingFinished() { return true; }

    default void mcLoadingTask(Runnable r1, Runnable r2) {
        if (r1 != null) r1.run();
        if (r2 != null) r2.run();
    }
}
