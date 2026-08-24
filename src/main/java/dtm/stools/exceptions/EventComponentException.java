package dtm.stools.exceptions;

public class EventComponentException extends RuntimeException {
    public EventComponentException(String message, Throwable cause) {
        super(message, cause);
    }
    public EventComponentException(String message) {
        super(message);
    }
}
