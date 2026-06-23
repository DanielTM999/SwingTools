package dtm.stools.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
public class InvalidClientSideElementException extends RuntimeException{

    private final String key;
    private final Object selectedElement;

    public InvalidClientSideElementException(String key, Object selectedElement) {
        super("Elemento inválido no client: key='" + key + "', valor='" + selectedElement + "'");
        this.key = key;
        this.selectedElement = selectedElement;
    }

    public InvalidClientSideElementException(String key, Object selectedElement, Throwable cause) {
        super("Elemento inválido no client: key='" + key + "', valor='" + selectedElement + "'", cause);
        this.key = key;
        this.selectedElement = selectedElement;
    }
}
