package dtm.stools.context;

import lombok.NonNull;
import java.awt.Component;
import java.util.List;

public interface IWindowComponent {
    default <T extends Component> T findById(@NonNull String id){return null;};
    default <T extends Component> List<T> findAllById(@NonNull String id){return null;};
    void reloadDomElements();
}
