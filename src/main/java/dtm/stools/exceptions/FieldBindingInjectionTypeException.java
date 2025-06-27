package dtm.stools.exceptions;

import lombok.Getter;

@Getter
public class FieldBindingInjectionTypeException extends RuntimeException {
    private final String fieldName;
    private final Class<?> expectedType;
    private final Class<?> actualType;
    private final Object actualValue;

    public FieldBindingInjectionTypeException(String fieldName, Class<?> expectedType, Class<?> actualType, Object actualValue) {
        super(String.format("Tipo incompatível para campo '%s': esperado '%s', recebido '%s', valor: %s",
                fieldName,
                expectedType.getName(),
                actualType == null ? "null" : actualType.getName(),
                actualValue));
        this.fieldName = fieldName;
        this.expectedType = expectedType;
        this.actualType = actualType;
        this.actualValue = actualValue;
    }
}
