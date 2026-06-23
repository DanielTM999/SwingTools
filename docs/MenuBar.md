# MenuBar

`MenuBar` e uma barra de menu Swing com API fluente, configuracao por schema, estilo e eventos.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.menu.bar` |
| Heranca | `MenuBar extends JMenuBar implements EventListenerComponent` |
| Uso principal | Menu de aplicacao desktop com acoes, estado e tema customizavel |

## Criacao fluente

```java
MenuBar bar = new MenuBar()
        .useModernDefaults()
        .menu("file", "Arquivo", file -> {
            file.addItem("open", "Abrir");
            file.addItem("save", "Salvar");
        })
        .menu("help", "Ajuda", help -> {
            help.addItem("about", "Sobre");
        });

frame.setJMenuBar(bar);
```

## API de menus

| Metodo | Uso |
|---|---|
| `addMenu(String id, String text)` | Cria menu |
| `menu(String id, String text)` | Retorna menu para construcao |
| `menu(String id, String text, Consumer<Menu>)` | Cria e configura |
| `load(MenuSchema)` | Carrega schema |
| `getMenu(String id)` | Busca menu |
| `getItem(String path)` | Busca item por path |
| `removeMenu(String id)` / `clearMenus()` | Remove |

## Estado de item

Use paths para alterar itens:

```java
bar.disable("file.save");
bar.setItemText("file.open", "Abrir arquivo");
bar.setItemSelected("view.sidebar", true);
```

## Eventos

Helpers retornam `EventSubscription`:

```java
bar.onItemClick(event -> {
    System.out.println(event.getItemId());
});
```

Eventos principais: `MENU_OPEN`, `MENU_CLOSE`, `BEFORE_ITEM_CLICK`, `ITEM_CLICK`.

## Estilo

APIs principais: `setStyle`, `style`, `useModernDefaults`, `useLookAndFeelStyle`, `useAccentGradient`, `setBackgroundGradient`, `setBarBackground`, `setBarForeground`, `setAccentColor`, `setMenuPadding`.

## Tipos

`MenuBarConfig`, `MenuBarStyle`, `MenuConfig`, `MenuItemConfig`, `MenuSchema`, `MenuAction`, `MenuBarEvent`, `EventMenuBar`, `MenuNode`, `MenuTreeEditor`, `GradientStop`.
