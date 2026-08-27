package dtm.stools.controllers;

import dtm.stools.activity.Activity;
import dtm.stools.activity.delegated.DelegatedWindow;
import dtm.stools.configs.SystemTrayConfiguration;
import dtm.stools.context.IWindow;
import dtm.stools.context.enums.TrayEventType;
import lombok.NonNull;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractWindowController<T extends IWindow> {
    private final AtomicReference<T> window;

    protected AbstractWindowController() {
        this.window = new AtomicReference<>();
    }

    public void onInit(T activity){ this.window.set(activity);}
    public void onLoad(T activity) throws Exception{}
    public void onClose(T activity) throws Exception{}
    public void onLostFocus(T activity) throws Exception{}
    public void onReciveEvent(T activity, Object args){}
    public void onSystemTrayClick(MouseEvent event, TrayEventType eventType, Activity currentActivity){}
    public void onResize(T activity){}
    public void applySystemTrayConfiguration(T activity, SystemTrayConfiguration systemTrayConfiguration) {}
    public void sendEvent(T activity, Object args){
        if(activity instanceof DelegatedWindow delegatedWindow){
            delegatedWindow.onReceiveEvent(args);
        }
    }
    public <S extends Component> S findById(@NonNull String id){
        T window = getWindow();
        return (window != null) ? window.findById(id) : null;
    };
    public <S extends Component> List<S> findAllById(@NonNull String id){
        T window = getWindow();
        return (window != null) ? window.findAllById(id) : null;
    };
    public final T getWindow(){ return window.get(); }

    @SuppressWarnings("unchecked")
    public final <S extends T> S getWindowAs(){
        return (S) getWindow();
    }

    public final <S extends T> S getWindowAs(final Class<S> ref){
        T window = getWindow();
        try{
            return ref.cast(window);
        }catch (Exception e){
            return null;
        }
    }


}
