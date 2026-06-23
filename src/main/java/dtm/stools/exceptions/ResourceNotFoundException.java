package dtm.stools.exceptions;

import lombok.Getter;

public class ResourceNotFoundException extends RuntimeException {

    @Getter
    private final String resourcePath;

    public ResourceNotFoundException(String message, String resourcePath) {
        super(message);
        this.resourcePath = resourcePath;
    }

    public ResourceNotFoundException(String message, String resourcePath, Throwable cause) {
        super(message, cause);
        this.resourcePath = resourcePath;
    }

}
