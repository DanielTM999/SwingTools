package dtm.stools.controllers.component;

import dtm.stools.context.IWindowComponent;
import dtm.stools.context.annotations.ClientRef;
import dtm.stools.context.annotations.ViewRef;
import dtm.stools.exceptions.FieldBindingInjectionTypeException;

import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class BindingAbstractViewController<T extends IWindowComponent> extends AbstractViewController<T>{


    @Override
    public void onInit(T view) {
        super.onInit(view);
        onBindingElements(view);
    }


    protected void onBindingElements(T view){
        try{
            Class<?> clazz = getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    onBindingElement(view, field);
                }
                clazz = clazz.getSuperclass();
            }
        }catch (CompletionException ce){
            Throwable cause = ce.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            } else if (cause != null) {
                throw new RuntimeException("Erro no binding dos campos", cause);
            } else {
                throw ce;
            }
        }
    }

    private void onBindingElement(T view, Field field){
        int mods = field.getModifiers();
        if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) return;

        if(field.isAnnotationPresent(ViewRef.class)){
            final String propRefName = getPropRefName(field);
            setField(field, () -> findViewRef(view, propRefName, field));
        }else if(field.isAnnotationPresent(ClientRef.class)){
            final String propRefName = getPropRefName(field);
            setField(field, () -> view.getFromClient(propRefName, null));
        }
    }

    private Object findViewRef(T view, String propRefName, Field field) {
        List<Component> components = view.findAllById(propRefName);
        if (components.isEmpty()) {
            return view.findById(propRefName);
        }

        Class<?> fieldType = field.getType();
        for (Component component : components) {
            if (fieldType.isAssignableFrom(component.getClass())) {
                return component;
            }
        }

        Component first = components.getFirst();
        throw new FieldBindingInjectionTypeException(field.getName(), fieldType, first.getClass(), first);
    }

    private void setField(Field field, Supplier<Object> action){
        try {
            if (!field.canAccess(this)) field.setAccessible(true);

            Object value = action.get();
            Class<?> fieldType = field.getType();

            if (value != null && !fieldType.isAssignableFrom(value.getClass())) {
                throw new FieldBindingInjectionTypeException(field.getName(), fieldType, value.getClass(), value);
            }

            field.set(this, value);

        }  catch (IllegalAccessException e) {
            throw new RuntimeException("Falha de acesso ao campo '" + field.getName() + "'", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Argumento inválido ao definir o campo '" + field.getName() + "'", e);
        } catch (Exception e) {
            throw new RuntimeException("Erro inesperado ao injetar valor no campo '" + field.getName() + "'", e);
        }
    }


    private String getPropRefName(Field field){
        String fieldName = field.getName();
        if(field.isAnnotationPresent(ViewRef.class)){
            ViewRef viewRef = field.getAnnotation(ViewRef.class);
            String viewRefValue = viewRef.value();
            return (viewRefValue != null && !viewRefValue.isEmpty()) ? viewRefValue : fieldName;
        }else if(field.isAnnotationPresent(ClientRef.class)){
            ClientRef clientRef = field.getAnnotation(ClientRef.class);
            String clientRefValue = clientRef.value();
            return (clientRefValue != null && !clientRefValue.isEmpty()) ? clientRefValue : fieldName;
        }
        return fieldName;
    }


}
