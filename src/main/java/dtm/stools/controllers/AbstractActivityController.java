package dtm.stools.controllers;

import dtm.stools.activity.Activity;

public abstract class AbstractActivityController {
    public abstract void onInit(Activity activity);
    public void onClose(Activity activity){}
    public void onLostFocus(Activity activity){}
}
