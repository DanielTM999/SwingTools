package dtm.stools.component.delegated;

import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.panels.KeyPanel;
import dtm.stools.controllers.component.AbstractViewController;
import dtm.stools.exceptions.DelegatedWindowException;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.lang.reflect.ParameterizedType;

@SuppressWarnings("unchecked")
public abstract class DelegatedKeyPanel<T extends AbstractViewController<KeyPanel>> extends KeyPanel implements DelegatedIWindowComponent{

    protected Class<T> controllerClass;
    protected T controller;

    public DelegatedKeyPanel(){
        delegateInit();
    }

    protected abstract T newController();

    @Override
    public void addNotify() {
        super.addNotify();
        reloadDomElements();
        onCreateController();
        getOrCreateController().onInit(this);
    }

    @Override
    public void disposeController(){
        this.controller = null;
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        getOrCreateController().onLoad(this);
    }

    @Override
    protected void onRemoved() {
        super.onRemoved();
        getOrCreateController().onRemoved(this);
    }

    @Override
    protected void onLostFocus(FocusEvent e) {
        super.onLostFocus(e);
        getOrCreateController().onLostFocus(this);
    }

    @Override
    protected void onFocus(FocusEvent e) {
        super.onFocus(e);
        getOrCreateController().onFocus(this);
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

    private T getOrCreateController() {
        if (controller == null) {
            controller = newController();

            if (controller == null) {
                throw new DelegatedWindowException(
                        "Falha ao obter o controller",
                        new NullPointerException("controller null")
                );
            }
        }

        return controller;
    }


}
