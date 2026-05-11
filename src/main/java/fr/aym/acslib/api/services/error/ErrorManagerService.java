// TODO port:1.20.1 stub - ErrorManagerService class (no-op implementation)
package fr.aym.acslib.api.services.error;

import fr.aym.acslib.api.ACsService;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger; // TODO port:1.20.1 - switched to slf4j to match Minecraft 1.20.1 logger.

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ErrorManagerService implements ACsService {
    public static final Comparator<ErrorData> ERROR_COMPARATOR = Comparator.comparingInt(e -> -e.getLevel().ordinal());

    private final Map<ResourceLocation, LocatedErrorList> errors = new LinkedHashMap<>();

    public ErrorCategory createErrorCategory(ResourceLocation location, String name) {
        return new ErrorCategory();
    }

    public void addError(String pack, ErrorCategory category, String genericType, ErrorLevel level, String object, String message) {
        addError(pack, category, genericType, level, object, message, null, 0);
    }

    public void addError(String pack, ErrorCategory category, String genericType, ErrorLevel level, String object, String message, Exception exception) {
        addError(pack, category, genericType, level, object, message, exception, 0);
    }

    public void addError(String pack, ErrorCategory category, String genericType, ErrorLevel level, String object, String message, Exception exception, int priority) {
        ResourceLocation key = new ResourceLocation("dynamx", pack == null ? "unknown" : pack.toLowerCase().replaceAll("[^a-z0-9_]", "_"));
        errors.computeIfAbsent(key, k -> new LocatedErrorList()).add(new ErrorData(genericType, level, object, message, exception, category));
    }

    public Map<ResourceLocation, LocatedErrorList> getAllErrors() {
        return errors;
    }

    public boolean hasErrors(ErrorCategory... categories) {
        return false; // TODO port:1.20.1 - placeholder
    }

    public void printErrors(Logger logger, List<ErrorCategory> categories, ErrorLevel minLevel) {
        // TODO port:1.20.1 stub
    }

    /**
     * Clears all errors for the given category.
     * TODO port:1.20.1 - kept as best-effort stub: the current implementation does not store
     * which category an error belongs to in the map keys, so we clear everything.
     */
    public void clear(ErrorCategory category) {
        errors.clear();
    }

    public static <K> Map<K, List<ErrorData>> groupBy(Collection<ErrorData> errorList, Function<ErrorData, K> keyExtractor) {
        return errorList.stream().collect(Collectors.groupingBy(keyExtractor));
    }

    @Override
    public String getName() { return "ErrorManager"; }

    @Override
    public String getVersion() { return "0.0.0"; }
}
