package dtm.stools.controllers.component;

import dtm.stools.context.IWindow;
import dtm.stools.context.IWindowComponent;

public abstract class AbstractViewController<T extends IWindowComponent>{
    protected T component;

    public void onInit(T component) {this.component = component;}
    public void onLoad(T component) {this.component = component;}
    public void onRemoved(T component) {}
    public void onLostFocus(T component) {}
    public void onFocus(T component) {}
}
