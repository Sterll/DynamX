// TODO port:1.20.1 stub - ErrorData class
package fr.aym.acslib.api.services.error;

public class ErrorData {
    private final String genericType;
    private final ErrorLevel level;
    private final String object;
    private final String message;
    private final Exception exception;
    private final ErrorCategory category;

    public ErrorData(String genericType, ErrorLevel level, String object, String message, Exception exception, ErrorCategory category) {
        this.genericType = genericType;
        this.level = level;
        this.object = object;
        this.message = message;
        this.exception = exception;
        this.category = category;
    }

    public String getGenericType() { return genericType; }
    public ErrorLevel getLevel() { return level; }
    public String getObject() { return object; }
    public String getMessage() { return message; }
    public Exception getException() { return exception; }
    public ErrorCategory getCategory() { return category; }
}
