package dtm.stools.activity.delegated;

import dtm.stools.activity.Activity;
import dtm.stools.configs.SystemTrayConfiguration;
import dtm.stools.context.enums.TrayEventType;
import dtm.stools.controllers.AbstractWindowController;
import dtm.stools.exceptions.DelegatedWindowException;

import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.ParameterizedType;

public abstract class DelegatedActivity<T extends AbstractWindowController<Activity>> extends Activity implements DelegatedWindow {

    protected Class<T> controllerClass;
    protected T controller;

    @SuppressWarnings("unchecked")
    public DelegatedActivity(){
        try {
            java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
            if (superclass instanceof ParameterizedType parameterizedType) {
                java.lang.reflect.Type[] classTypes = parameterizedType.getActualTypeArguments();
                if (classTypes.length > 0) {
                    java.lang.reflect.Type type = classTypes[0];

                    if (type instanceof Class<?>) {
                        controllerClass = (Class<T>) type;
                    } else if (type instanceof ParameterizedType pt) {
                        controllerClass = (Class<T>) pt.getRawType();
                    }
                }
            }
        }catch (Exception e){
            throw new DelegatedWindowException("Falha ao obter o tipo de controller", e);
        }
    }

    protected abstract T newController();

    @Override
    public void sendEvent(Object eventArgs){
        resolveController();
        if(controller != null) controller.onReciveEvent(this, eventArgs);
    }

    @Override
    public void init() {
        super.init();
        resolveController();
        controller.onInit(this);
    }

    @Override
    protected void onClose(WindowEvent e) throws Exception{
        super.onClose(e);
        resolveController();
        controller.onClose(this);
    }

    @Override
    protected void onLostFocus(WindowEvent e) throws Exception{
        super.onLostFocus(e);
        resolveController();
        controller.onLostFocus(this);
    }

    @Override
    protected void onLoad(WindowEvent e) throws Exception{
        super.onLoad(e);
        resolveController();
        this.controller.onLoad(this);
    }

    @Override
    protected void onSystemTrayClick(MouseEvent event, TrayEventType eventType, Activity currentActivity) {
        super.onSystemTrayClick(event, eventType, currentActivity);
        resolveController();
        this.controller.onSystemTrayClick(event, eventType, currentActivity);
    }

    @Override
    protected void onResize() {
        super.onResize();
        resolveController();
        this.controller.onResize(this);
    }

    @Override
    protected void applySystemTrayConfiguration(SystemTrayConfiguration systemTrayConfiguration) {
        super.applySystemTrayConfiguration(systemTrayConfiguration);
        resolveController();
        this.controller.applySystemTrayConfiguration(this, systemTrayConfiguration);
    }

    private void onCreateController(){
        controller = newController();
        if(controller == null)  throw new DelegatedWindowException("Falha ao obter o controller", new NullPointerException("controller null"));
    }

    private void resolveController(){
        if(controller == null){
            onCreateController();
        }
    }
}
