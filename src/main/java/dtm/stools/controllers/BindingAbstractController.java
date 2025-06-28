package dtm.stools.controllers;

import dtm.stools.context.IWindow;
import dtm.stools.context.annotations.ClientRef;
import dtm.stools.context.annotations.ViewRef;
import dtm.stools.exceptions.FieldBindingInjectionTypeException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public abstract class BindingAbstractController<T extends IWindow> extends AbstractController<T>{

    @Override
    public void onInit(T activity) {
        onBindingElements(activity);
    }

    protected void onBindingElements(T activity){
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        try(ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()){
            Class<?> clazz = getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    CompletableFuture<?> task = CompletableFuture.runAsync(() -> onBindingElement(activity, field), executorService);
                    tasks.add(task);
                }
                clazz = clazz.getSuperclass();
            }
            CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
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

    private void onBindingElement(T activity, Field field){
        int mods = field.getModifiers();
        if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) return;

        if(field.isAnnotationPresent(ViewRef.class)){
            final String propRefName = getPropRefName(field);
            setField(field, () -> activity.findById(propRefName));
        }else if(field.isAnnotationPresent(ClientRef.class)){
            final String propRefName = getPropRefName(field);
            setField(field, () -> activity.getFromClient(propRefName, null));
        }
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

}
