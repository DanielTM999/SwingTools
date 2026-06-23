package dtm.stools.exceptions;

public class FileSelectionException extends RuntimeException {
    private final String title;

    public FileSelectionException(String title, String message) {
        super(message);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}