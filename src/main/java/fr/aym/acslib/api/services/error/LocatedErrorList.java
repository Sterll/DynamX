// TODO port:1.20.1 stub - LocatedErrorList class
package fr.aym.acslib.api.services.error;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LocatedErrorList {
    private final List<ErrorData> errors = new ArrayList<>();

    public Collection<ErrorData> getErrors() { return errors; }

    public ErrorLevel getHighestErrorLevel() {
        ErrorLevel max = ErrorLevel.ADVICE;
        for (ErrorData d : errors) {
            if (d.getLevel().ordinal() > max.ordinal()) max = d.getLevel();
        }
        return max;
    }

    public void add(ErrorData data) {
        errors.add(data);
    }
}
