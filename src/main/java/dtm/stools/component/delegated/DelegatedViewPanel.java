package dtm.stools.component.delegated;

import dtm.stools.component.ViewPanel;
import dtm.stools.controllers.component.AbstractViewController;
import dtm.stools.exceptions.DelegatedWindowException;

import java.lang.reflect.ParameterizedType;

@SuppressWarnings("unchecked")
public abstract class DelegatedViewPanel<T extends AbstractViewController<ViewPanel>> extends ViewPanel implements DelegatedIWindowComponent{

    protected Class<T> controllerClass;
    protected T controller;

    public DelegatedViewPanel(){
        delegateInit();
    }

    protected abstract T newController();

    @Override
    public void addNotify() {
        super.addNotify();
        onDrawing();
        reloadDomElements();
        onCreateController();
    }

    @Override
    public void disposeController(){
        this.controller = null;
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        controller.onLoad();
    }

    @Override
    protected void onRemoved() {
        super.onRemoved();
        controller.onRemoved();
    }

    @Override
    protected void onLostFocus() {
        super.onLostFocus();
        controller.onLostFocus();
    }

    @Override
    protected void onFocus() {
        super.onFocus();
        controller.onFocus();
    }

    private void onCreateController(){
        if(controller == null) {
            controller = newController();
            if(controller == null) {
                throw new DelegatedWindowException("Falha ao obter o controller", new NullPointerException("controller null"));
            }
        }
    }

    private void delegateInit(){
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

}
