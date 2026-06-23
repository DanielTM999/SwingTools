package dtm.stools.component.menu.popup.style;

import javax.swing.border.Border;

@FunctionalInterface
public interface BorderFactorySupplier {
    Border create();
}
