# CollapsibleMenuBar

`CollapsibleMenuBar` e uma variacao de `MenuBar` que pode esconder/mostrar os menus por um botao de colapso.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.menu.bar` |
| Heranca | `CollapsibleMenuBar extends MenuBar` |
| Uso principal | Economizar espaco horizontal mantendo uma barra de menu completa |

## Heranca

Tudo que `MenuBar` oferece continua disponivel: schema, menus fluentes, eventos, estilo, brand e componentes antes/depois do menu.

## API de colapso

| Metodo | Uso |
|---|---|
| `setCollapsed(boolean)` | Define estado |
| `toggleCollapsed()` | Alterna estado |
| `isCollapsed()` | Consulta estado |
| `setAutoCollapseAfterMenuClose(boolean)` | Recolhe apos fechar menu |
| `setCollapseButtonVisibleWhenExpanded(boolean)` | Mantem botao visivel expandido |
| `getCollapseButton()` | Acesso ao botao |

## Botao

| Metodo | Uso |
|---|---|
| `collapseButton(Consumer<JButton>)` | Customizacao direta |
| `setCollapseButtonResource(...)` | Icone por recurso |
| `setCollapseButtonImage(Image)` | Icone por imagem |
| `setCollapseButtonIcon(Icon)` | Icone Swing |
| `setCollapseButtonColor(Color)` | Cor do icone |
| `setCollapseButtonIconSize(int)` | Tamanho do icone |
| `setCollapseButtonSize(int)` | Tamanho do botao |

## Exemplo

```java
CollapsibleMenuBar bar = new CollapsibleMenuBar()
        .setCollapsed(true)
        .setAutoCollapseAfterMenuClose(true);

bar.menu("file", "Arquivo", file -> {
    file.addItem("open", "Abrir");
});
frame.setJMenuBar(bar);
```

## Cuidados

- Configure menus pela API herdada de `MenuBar`.
- Use `preMenu` quando precisar colocar componentes junto ao botao de colapso.
