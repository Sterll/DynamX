// TODO port:1.20.1 stub - ErrorFormatter functional interface
package fr.aym.acslib.api.services.error;

import java.util.List;

@FunctionalInterface
public interface ErrorFormatter {
    void formatError(StringBuilder errorBuilder, boolean addStackTrace, List<ErrorData> errors);
}
