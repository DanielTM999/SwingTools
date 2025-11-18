package dtm.stools.component.grids.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define uma coluna para a DataTable, permitindo controlo sobre o nome,
 * ordem e visibilidade, lendo os atributos de um POJO.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD) // Esta anotação só pode ser usada em atributos de classe
public @interface GridColumn {
    /**
     * O nome (título) que aparecerá no cabeçalho da coluna.
     */
    String name() default "";

    /**
     * A ordem em que esta coluna deve aparecer (menor para maior).
     * Campos sem anotação ou com a mesma ordem são ignorados
     * (ou ordenados por nome, dependendo da implementação).
     */
    int order() default 999;

    boolean editable() default true;
    int width() default 100;
    boolean visible() default true;
}
