package fr.dynamx.utils.optimization;

import fr.dynamx.DynamX;
import net.minecraft.ChatFormatting;

/**
 * Class pool utility for optimization <br>
 * Permits recycling objects instead of filling the memory
 */
public abstract class ClassPool<T> {
    protected final int capacityWarning;

    protected T[] pool;
    protected SubClassPool<T> root;
    protected int subPoolCount;
    protected int sizeWarnings;

    public ClassPool(int capacityWarning, int initialCapacity) {
        this.capacityWarning = capacityWarning;
        this.pool = createNewPool(0, initialCapacity);
    }

    /**
     * Opens a sub pool, all objects affected after this called will be released once you call closeSubPool
     */
    public void openSubPool(String identifier) {
        if (root == null) {
            root = new SubClassPool<>(null, 0, identifier);
        } else {
            root = new SubClassPool<>(root, root.getStartIndex() + root.getAffectedObjectsCount(), identifier);
        }
        subPoolCount++;
    }

    /**
     * Closes a sub pool and releases all previously affected objects
     */
    public void closeSubPool() {
        if (root != null) {
            //root.onClose();
            root = root.getParent();
            subPoolCount--;
        } else {
            DynamX.LOGGER.warn("Tried to close a pool that was not opened", new IllegalStateException("Tried to close a pool that was not opened"));
        }
    }

    /**
     * Recycles an instance or enlarges the pool with new clean instances, then returns one of them
     */
    public T provideNewInstance() {
        T instance;
        if (root == null) {
            // TODO port:1.20.1 - The 1.12 codebase opened these pools explicitly on every entry
            //  point; several of those sites (createShape during entity init, RenderRagdoll, ...)
            //  haven't been re-wired in the port. Auto-opening a default sub-pool keeps behaviour
            //  correct; the spammy stack-traced warning is dropped so it doesn't fill the console
            //  at 60Hz per entity.
            openSubPool(SubClassPool.DEFAULT_DEFAULT);
        }
        if (root.getStartIndex() + root.getAffectedObjectsCount() >= pool.length) //If the pool is too small
        {
            T[] nPool = createNewPool(pool.length, root.getStartIndex() + root.getAffectedObjectsCount() + getGrowthSize()); //Allocate a bigger pool
            System.arraycopy(pool, 0, nPool, 0, pool.length);
            pool = nPool;

            if (pool.length > capacityWarning) {
                // TODO port:1.20.1 - Throttled to avoid 60Hz spam when ClassPool auto-opens
                //  DEFAULT_DEFAULT (see provideNewInstance above). Real fix is to wrap every pool
                //  consumer in openPool/closePool; until then, warn at doubling intervals and only
                //  dump a single stack so the cause stays discoverable without flooding the log.
                if (Integer.bitCount(sizeWarnings) == 1 || sizeWarnings == 0) {
                    DynamX.LOGGER.warn("Optimization issue : Pool is very large : {} ! open c {} of type {}", pool.length, subPoolCount, this);
                    if (sizeWarnings == 0) {
                        Thread.dumpStack();
                    }
                }
                sizeWarnings++;
            }
        }
        instance = pool[root.getStartIndex() + root.getAffectedObjectsCount()]; //Take an unused instance
        root.affectObject(instance); //Instance is now used
        return instance;
    }

    /**
     * Used to enlarge the pool, not called often
     *
     * @return A array, empty from 0 to newInstancesStart-1, containing fresh instances from newInstancesStart to size
     */
    public abstract T[] createNewPool(int newInstancesStart, int size);

    /**
     * @return The number of "slots" too allocate when pool is too small
     */
    public abstract int getGrowthSize();

    public int getCurrentPoolObjectCount() {
        return root == null ? -10 : root.getAffectedObjectsCount();
    }

    public int getTotalAffectedObject() {
        if (root != null) {
            return root.getStartIndex() + root.getAffectedObjectsCount();
        }
        return 0;
    }

    public int getUnaffectedObjectsCount() {
        return pool.length - getTotalAffectedObject();
    }

    public String getDebugInfo() {
        return "spc=" + subPoolCount + ", tt=" + pool.length + ", used=" + getCurrentPoolObjectCount() + ", tt_used=" + getTotalAffectedObject() + ", tt_free=" + getUnaffectedObjectsCount();
    }

    public String getExpandedDebugInfo() {
        if(root == null) {
            return "empty";
        }
        int curDepth = subPoolCount;
        StringBuilder result = new StringBuilder();
        SubClassPool<T> current = root;
        int maxDepth = curDepth - 400;
        if(curDepth > 400) {
            result.append(ChatFormatting.RED).append("Current pool depth is larger than 400. You have a leak somewhere.").append("\n");
        }
        while (current != null && curDepth > maxDepth) {
            result.append("At: " ).append(curDepth).append(": ").append(current).append("\n");
            current = current.getParent();
            curDepth -= 1;
        }
        return result.toString();
    }
}
