package dtm.stools.component.panels.editor.powerpoint.api;

import dtm.stools.component.panels.editor.powerpoint.model.Presentation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/** Editable presentation state; callers use the Swing event dispatch thread. */
public final class PowerPointSession {
    private Presentation presentation=Presentation.create();
    private final ArrayDeque<Presentation> undo=new ArrayDeque<>(), redo=new ArrayDeque<>();
    private final List<Runnable> listeners=new ArrayList<>();
    private int selectedSlide;
    private String selectedObjectId;
    private long revision, savedRevision;
    private int historyLimit=200;
    private boolean readOnly;
    private java.util.function.Predicate<Presentation> editValidator=value->true;
    public void setEditValidator(java.util.function.Predicate<Presentation> validator){editValidator=Objects.requireNonNull(validator);}
    public Presentation getPresentation(){return presentation;}
    public long revision(){return revision;}
    public int selectedSlide(){return selectedSlide;}
    public String selectedObjectId(){return selectedObjectId;}
    public boolean isDirty(){return revision!=savedRevision;}
    public boolean canUndo(){return !undo.isEmpty();}
    public boolean canRedo(){return !redo.isEmpty();}
    public void setHistoryLimit(int value){if(value<0)throw new IllegalArgumentException();historyLimit=value;trim();}
    public void setReadOnly(boolean value){readOnly=value;fire();}
    public boolean isReadOnly(){return readOnly;}
    public AutoCloseable addListener(Runnable listener){listeners.add(listener);return ()->listeners.remove(listener);}
    private void fire(){for(Runnable listener:List.copyOf(listeners))listener.run();}
    private void trim(){while(undo.size()>historyLimit)undo.removeLast();}
    public void replace(Presentation value){
        presentation=Objects.requireNonNull(value);undo.clear();redo.clear();selectedSlide=0;selectedObjectId=null;
        revision++;savedRevision=revision;fire();
    }
    public void edit(UnaryOperator<Presentation> operation){
        if(readOnly)throw new IllegalStateException("Presentation is read only");
        Presentation next=Objects.requireNonNull(operation.apply(presentation));
        if(next==presentation || next.equals(presentation))return;
        if(!editValidator.test(next))return;
        undo.push(presentation);trim();redo.clear();presentation=next;revision++;fire();
    }
    public void undo(){if(readOnly||undo.isEmpty())return;redo.push(presentation);presentation=undo.pop();revision++;clampSelection();fire();}
    public void redo(){if(readOnly||redo.isEmpty())return;undo.push(presentation);presentation=redo.pop();revision++;clampSelection();fire();}
    public void selectSlide(int index){
        if(index<0||index>=presentation.slides().size())throw new IndexOutOfBoundsException(index);
        selectedSlide=index;selectedObjectId=null;fire();
    }
    public void selectObject(String id){selectedObjectId=id;fire();}
    private void clampSelection(){selectedSlide=Math.min(selectedSlide,presentation.slides().size()-1);selectedObjectId=null;}
    public void markSaved(long snapshotRevision){if(snapshotRevision==revision){savedRevision=revision;fire();}}
}
