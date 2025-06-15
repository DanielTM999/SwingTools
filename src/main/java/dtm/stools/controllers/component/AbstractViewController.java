package dtm.stools.controllers.component;

import dtm.stools.context.IWindow;
import dtm.stools.context.IWindowComponent;

public abstract class AbstractViewController<T extends IWindowComponent>{
    public void onLoad() {}
    public void onRemoved() {}
    public void onLostFocus() {}
    public void onFocus() {}
}
