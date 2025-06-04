package dtm.stools.models;

import dtm.stools.core.DomViewer;
import lombok.Data;
import lombok.NonNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class DomViewerConcret implements DomViewer {

    private final String name;
    private final Component root;
    private final List<DomViewer> viewerList;

    public DomViewerConcret(Component root, String name){
        this.root = root;
        this.viewerList = Collections.synchronizedList(new ArrayList<>());
        this.name = name;
    }

    @Override
    public Component getRoot() {
        return root;
    }

    @Override
    public List<DomViewer> getDomViewer() {
        return viewerList;
    }

    public void add(@NonNull DomViewer domViewer){
        viewerList.add(domViewer);
    }


}
