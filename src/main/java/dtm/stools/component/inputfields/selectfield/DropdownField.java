package dtm.stools.component.inputfields.selectfield;

import dtm.stools.component.events.EventType;

import javax.swing.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;

public class DropdownField extends DropdownFieldListener<Object> {

    private Function<Object, String> displayFunction = Objects::toString;
    private ListCellRenderer<? super Object> customRenderer = null;
    private String placeholder;

    public DropdownField(){
        applyRenderer();
        configInternalChangeEvent();
    }

    public DropdownField(Object... dataSource){
        configInternalChangeEvent();
        setDataSource(dataSource);
    }

    public <T> DropdownField(Collection<T> dataSource){
        configInternalChangeEvent();
        setDataSource(dataSource);
    }

    public void setDisplayText(Function<Object, String> displayFunction){
        this.displayFunction = (displayFunction != null) ? displayFunction : Objects::toString;
        applyRenderer();
        repaint();
    }

    public <T> void setDataSource(Collection<T> dataSource){
        if(dataSource == null) {
            setModel(new DefaultComboBoxModel<>());
            return;
        }

        setModel(new DefaultComboBoxModel<>(dataSource.toArray(Object[]::new)));
        setSelectedItem(null);
    }

    public void setDataSource(Object... dataSource){
        setModel(new DefaultComboBoxModel<>(dataSource));
        setSelectedItem(null);
    }

    public void setCustomRenderer(ListCellRenderer<? super Object> renderer){
        this.customRenderer = renderer;
        applyRenderer();
    }

    public boolean select(Object value){
        if(!contains(value)) return false;

        setSelectedItem(value);
        dispachEvent(EventType.CHANGE, this::getSelectedItem);
        return true;
    }

    public boolean isEmpty(){
        return getItemCount() == 0;
    }

    public int getItemSize(){
        return getItemCount();
    }

    public void clear(){
        setModel(new DefaultComboBoxModel<>());
        setSelectedItem(null);
        dispachEvent(EventType.CLEAR, () -> null);
    }

    public boolean contains(Object value){
        return IntStream.range(0, getItemCount())
                .mapToObj(this::getItemAt)
                .anyMatch(o -> Objects.equals(o, value));
    }

    public void selectFirst(){
        if(getItemCount() > 0){
            setSelectedIndex(0);
            dispachEvent(EventType.CHANGE, this::getSelectedItem);
        }
    }

    public void selectLast(){
        if(getItemCount() > 0){
            setSelectedIndex(getItemCount() - 1);
            dispachEvent(EventType.CHANGE, this::getSelectedItem);
        }
    }

    public void reload(){
        applyRenderer();
        repaint();
    }

    public List<Object> getDataSource(){
        int size = getItemCount();
        return IntStream.range(0, size)
                .mapToObj(this::getItemAt)
                .toList();
    }

    public void setPlaceholder(String placeholder){
        this.placeholder = placeholder;
        applyRenderer();
    }


    @SuppressWarnings("unchecked")
    public <T> List<T> getDataSource(Class<T> type) {
        int size = getItemCount();
        return IntStream.range(0, size)
                .mapToObj(this::getItemAt)
                .filter(type::isInstance)
                .map(o -> (T) o)
                .toList();
    }

    private void applyRenderer(){
        if(customRenderer != null){
            super.setRenderer(customRenderer);
            return;
        }

        super.setRenderer(new DefaultListCellRenderer(){
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                if (value == null && index == -1 && placeholder != null) {
                    return super.getListCellRendererComponent(list, placeholder, index, isSelected, cellHasFocus);
                }

                String text;
                try {
                    text = (value != null) ? displayFunction.apply(value) : "";
                } catch (Exception ex) {
                    text = "";
                }

                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
    }

    private void configInternalChangeEvent() {
        this.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                dispachEvent(EventType.CHANGE, this::getSelectedItem);
            }
        });
    }

}
