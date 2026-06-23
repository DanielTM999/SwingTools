package dtm.stools.component.menu.bar.config;

import dtm.stools.component.menu.bar.MenuBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MenuSchema {
    private final List<MenuConfig> menus = new ArrayList<>();

    public static MenuSchema of(MenuConfig... menus) {
        MenuSchema schema = new MenuSchema();
        if (menus != null) {
            Arrays.stream(menus).filter(Objects::nonNull).forEach(schema::menu);
        }
        return schema;
    }

    public MenuSchema menu(MenuConfig menu) {
        if (menu != null) menus.add(menu);
        return this;
    }

    public MenuSchema menu(String id, String text, Consumer<MenuBar.Menu> builder) {
        return menu(new MenuConfig(id, text).build(builder));
    }

    public List<MenuConfig> getMenus() {
        return List.copyOf(menus);
    }
}
