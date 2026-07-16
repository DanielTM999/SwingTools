package dtm.stools.component.grids.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GridColumn {

    String name() default "";

    int order() default 999;

    boolean editable() default true;
    int width() default 100;
    boolean visible() default true;
    String setterRef() default "";
}
