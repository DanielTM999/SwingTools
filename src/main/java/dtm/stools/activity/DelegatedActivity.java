package dtm.stools.activity;

import dtm.stools.controllers.AbstractActivityController;
import dtm.stools.exceptions.DelegatedWindowException;

import java.lang.reflect.ParameterizedType;

public abstract class DelegatedActivity<T extends AbstractActivityController> extends Activity{

    protected Class<T> controllerClass;
    protected T controller;

    @SuppressWarnings("unchecked")
    public DelegatedActivity(){
        try {
            java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
            if (superclass instanceof ParameterizedType parameterizedType) {
                java.lang.reflect.Type type = parameterizedType.getActualTypeArguments()[0];
                controllerClass = (Class<T>) type;
            }
        }catch (Exception e){
            throw new DelegatedWindowException("Falha ao obter o controller", e);
        }

        onCreateController();
    }

    protected abstract T newController();

    @Override
    public void init() {
        super.init();
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

    private void onCreateController(){
        controller = newController();
        if(controller == null)  throw new DelegatedWindowException("Falha ao obter o controller", new NullPointerException("controller null"));
    }
}
