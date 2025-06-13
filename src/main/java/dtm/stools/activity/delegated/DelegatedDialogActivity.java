package dtm.stools.activity.delegated;

import dtm.stools.activity.Activity;
import dtm.stools.activity.DialogActivity;
import dtm.stools.controllers.AbstractController;
import dtm.stools.exceptions.DelegatedWindowException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class DelegatedDialogActivity<T extends AbstractController<DialogActivity>> extends DialogActivity implements DelegatedWindow {

    protected Class<T> controllerClass;
    protected T controller;

    @SuppressWarnings("unchecked")
    public DelegatedDialogActivity(){
        try {
            java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
            if (superclass instanceof ParameterizedType parameterizedType) {
                java.lang.reflect.Type[] classTypes = parameterizedType.getActualTypeArguments();
                java.lang.reflect.Type type = (classTypes.length > 0) ? classTypes[0] : null;
                controllerClass = (Class<T>) type;
            }
        }catch (Exception e){
            throw new DelegatedWindowException("Falha ao obter o tipo de controller", e);
        }
    }

    protected abstract T newController();

    @Override
    public void sendServerEvent(Object eventArgs){
        if(controller != null) controller.onReciveWindowEvent(this, eventArgs);
    }

    @Override
    public void init() {
        super.init();
        onCreateController();
        controller.onInit(this);
    }

    @Override
    protected void onClose() {
        super.onClose();
        controller.onClose(this);
    }

    @Override
    protected void onLostFocus() {
        super.onLostFocus();
        controller.onLostFocus(this);
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        this.controller.onLoad(this);
    }

    private void onCreateController(){
        controller = newController();
        if(controller == null)  throw new DelegatedWindowException("Falha ao obter o controller", new NullPointerException("controller null"));
    }

}
