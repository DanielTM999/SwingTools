package dtm.stools.component.panels.dock;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DockLayoutSnapshot {
    private final Map<DockRegion, List<String>> keysByRegion = new EnumMap<>(DockRegion.class);
    private final Map<DockRegion, String> selectedKeys = new EnumMap<>(DockRegion.class);
    private final Map<DockRegion, Dimension> preferredSizes = new EnumMap<>(DockRegion.class);

    public DockLayoutSnapshot() {
        for (DockRegion region : DockRegion.values()) {
            keysByRegion.put(region, new ArrayList<>());
        }
    }

    void putRegionKeys(DockRegion region, List<String> keys) {
        keysByRegion.put(region, new ArrayList<>(keys));
    }

    void putSelectedKey(DockRegion region, String key) {
        if (key == null) {
            selectedKeys.remove(region);
        } else {
            selectedKeys.put(region, key);
        }
    }

    void putPreferredSize(DockRegion region, Dimension size) {
        if (size == null) {
            preferredSizes.remove(region);
        } else {
            preferredSizes.put(region, new Dimension(size));
        }
    }

    public List<String> getKeys(DockRegion region) {
        List<String> keys = keysByRegion.get(region);
        return keys == null ? List.of() : new ArrayList<>(keys);
    }

    public Map<DockRegion, List<String>> getKeysByRegion() {
        Map<DockRegion, List<String>> copy = new LinkedHashMap<>();
        for (DockRegion region : DockRegion.values()) {
            copy.put(region, getKeys(region));
        }
        return copy;
    }

    public String getSelectedKey(DockRegion region) {
        return selectedKeys.get(region);
    }

    public Map<DockRegion, String> getSelectedKeys() {
        return new LinkedHashMap<>(selectedKeys);
    }

    public Dimension getPreferredSize(DockRegion region) {
        Dimension size = preferredSizes.get(region);
        return size == null ? null : new Dimension(size);
    }

    public Map<DockRegion, Dimension> getPreferredSizes() {
        Map<DockRegion, Dimension> copy = new LinkedHashMap<>();
        for (Map.Entry<DockRegion, Dimension> entry : preferredSizes.entrySet()) {
            copy.put(entry.getKey(), new Dimension(entry.getValue()));
        }
        return copy;
    }
}
