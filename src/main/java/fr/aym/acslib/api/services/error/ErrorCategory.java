// TODO port:1.20.1 stub - ErrorCategory class
package fr.aym.acslib.api.services.error;

import java.util.HashMap;
import java.util.Map;

public class ErrorCategory {
    private final Map<String, ErrorFormatter> formatters = new HashMap<>();

    public void registerErrorFormatter(String genericType, ErrorFormatter formatter) {
        formatters.put(genericType, formatter);
    }

    public ErrorFormatter getErrorFormatter(String genericType) {
        return formatters.getOrDefault(genericType, (sb, addStack, errors) -> {});
    }
}
