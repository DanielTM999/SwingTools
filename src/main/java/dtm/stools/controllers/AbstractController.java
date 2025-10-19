package dtm.stools.controllers;

import dtm.stools.activity.Activity;
import dtm.stools.activity.delegated.DelegatedActivity;
import dtm.stools.activity.delegated.DelegatedWindow;
import dtm.stools.context.IWindow;

public abstract class AbstractController<T extends IWindow> {
    public abstract void onInit(T activity);
    public void onLoad(T activity) throws Exception{}
    public void onClose(T activity) throws Exception{}
    public void onLostFocus(T activity) throws Exception{}
    public void onReciveEvent(T activity, Object args){}
    public void sendEvent(T activity, Object args){
        if(activity instanceof DelegatedWindow delegatedWindow){
            delegatedWindow.onRecieveEvent(args);
        }
    }
}
