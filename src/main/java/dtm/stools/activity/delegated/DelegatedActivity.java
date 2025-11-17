package dtm.stools.activity.delegated;

import dtm.stools.activity.Activity;
import dtm.stools.context.enums.TrayEventType;
import dtm.stools.controllers.AbstractController;
import dtm.stools.exceptions.DelegatedWindowException;

import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class DelegatedActivity<T extends AbstractController<Activity>> extends Activity implements DelegatedWindow {

    protected Class<T> controllerClass;
    protected T controller;

    @SuppressWarnings("unchecked")
    public DelegatedActivity(){
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
    public void sendEvent(Object eventArgs){
        if(controller != null) controller.onReciveEvent(this, eventArgs);
    }

    @Override
    public void init() {
        super.init();
        onCreateController();
        controller.onInit(this);
    }

    @Override
    protected void onClose(WindowEvent e) throws Exception{
        super.onClose(e);
        controller.onClose(this);
    }

    @Override
    protected void onLostFocus(WindowEvent e) throws Exception{
        super.onLostFocus(e);
        controller.onLostFocus(this);
    }

    @Override
    protected void onLoad(WindowEvent e) throws Exception{
        super.onLoad(e);
        this.controller.onLoad(this);
    }

    @Override
    protected void onSystemTrayClick(MouseEvent event, TrayEventType eventType, Activity currentActivity) {
        super.onSystemTrayClick(event, eventType, currentActivity);
        this.controller.onSystemTrayClick(event, eventType, currentActivity);
    }

    private void onCreateController(){
        controller = newController();
        if(controller == null)  throw new DelegatedWindowException("Falha ao obter o controller", new NullPointerException("controller null"));
    }

}
