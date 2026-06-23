# ActionPopupMenu

`ActionPopupMenu` e um `JPopupMenu` com API fluente para criar menus de contexto.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.menu.popup` |
| Heranca | `ActionPopupMenu extends JPopupMenu implements ActionMenuSupport<ActionPopupMenu>` |
| Uso principal | Menus de contexto com item, icone, submenu, checkbox, radio, separador e estilo |

## Criacao

```java
ActionPopupMenu menu = ActionPopupMenu.create()
        .item("Abrir", e -> open())
        .separator()
        .checkItem("Mostrar ocultos", true, e -> toggleHidden())
        .submenu("Novo", sub -> sub
                .item("Arquivo", e -> createFile())
                .item("Pasta", e -> createFolder()));

component.setComponentPopupMenu(menu);
```

## Itens

| Metodo | Uso |
|---|---|
| `item(String, ActionListener)` | Item simples |
| `item(String, Icon, ActionListener)` | Item com icone |
| `item(String, boolean, ActionListener)` | Item habilitado/desabilitado |
| `item(Action)` / `item(JMenuItem)` | Reuso Swing |
| `checkItem(...)` | Checkbox |
| `radioItem(...)` | Radio button |
| `submenu(...)` | Submenu fluente |
| `custom(Component)` | Componente customizado |
| `separator()` | Separador |
| `when(...)` | Adicao condicional |

## Estilo e tamanho

```java
ActionPopupMenu.create()
        .popupSize(260, 260)
        .background(new Color(35, 35, 38))
        .foreground(new Color(230, 230, 230))
        .selectionBackground(new Color(70, 70, 76))
        .selectionForeground(Color.WHITE)
        .enableRootStyleForChildren();
```

APIs relacionadas: `style`, `border`, `preferredPopupWidth`, `preferredPopupHeight`, `minPopupSize`, `maxPopupSize`, `preferredPopupSize`.

## Exibicao manual

```java
menu.showAt(button, 0, button.getHeight());
menu.showAt(button, new Point(8, 8));
```

## Cuidados

- Para menu padrao de componente, prefira `setComponentPopupMenu(menu)`.
- Use `enableRootStyleForChildren()` quando submenus devem herdar estilo do root.
- A API fluente retorna o proprio menu para encadeamento.
