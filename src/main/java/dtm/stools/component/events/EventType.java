package dtm.stools.component.events;

import dtm.stools.component.grids.DataTableListener;

public final class EventType {
    public static final String CHANGE = "change";
    public static final String INPUT = "input";
    public static final String LOAD = "load";
    public static final String CLEAR = "clear";


    class GridViewTable {
        public static final String SELECTION_EVENT = "SELECTION";
        public static final String ACTION_EVENT = "ACTION";
        public static final String DATA_CHANGED_EVENT = "DATA_CHANGED";
        public static final String ADD_REQUEST_EVENT = "ADD_REQUEST";
        public static final String DELETE_REQUEST_EVENT = "DELETE_REQUEST";
        public static final String CELL_VALUE_CHANGED_EVENT = "CELL_VALUE_CHANGED";
    }

}
