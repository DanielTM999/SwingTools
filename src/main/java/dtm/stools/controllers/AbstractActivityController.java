package dtm.stools.controllers;

import dtm.stools.activity.Activity;
import dtm.stools.activity.DelegatedActivity;

public abstract class AbstractActivityController {
    public abstract void onInit(Activity activity);
    public void onLoad(Activity activity){}
    public void onClose(Activity activity){}
    public void onLostFocus(Activity activity){}
    public void onReciveWindowEvent(Activity activity, Object args){}
    public void sendWindowEvent(Activity activity, Object args){
        if(activity instanceof DelegatedActivity<?> delegatedActivity){
            delegatedActivity.onRecieveServerEvent(args
            );
        }
    }
}
