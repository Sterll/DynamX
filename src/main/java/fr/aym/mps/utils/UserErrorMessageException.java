// TODO port:1.20.1 stub - UserErrorMessageException
package fr.aym.mps.utils;

public class UserErrorMessageException extends RuntimeException {
    private final String title;
    private final String[] messages;

    public UserErrorMessageException(String message, Throwable cause) {
        super(message, cause);
        this.title = message;
        this.messages = new String[0];
    }

    public UserErrorMessageException(String message, Throwable cause, String title, String... messages) {
        super(message, cause);
        this.title = title;
        this.messages = messages;
    }

    public String getTitle() { return title; }
    public String[] getMessages() { return messages; }
}
