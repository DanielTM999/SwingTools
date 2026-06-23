# TabbedPanel

`TabbedPanel` e um wrapper avancado de `JTabbedPane` com abas identificadas por chave, eventos, estado visual e suporte a drag/split.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.tab` |
| Heranca | `TabbedPanel extends PanelEventListener` |
| Unidade | `TabEntry`, identificada por `key` |
| Uso principal | Abas de documentos, areas de trabalho, editores e paineis de ferramenta |

## Heranca

```text
ViewPanel
  BlockingPanel
    PanelEventListener
      TabbedPanel
```

Por herdar `PanelEventListener`, tambem suporta `lockUI`, `unlockUI`, eventos, DOM local e estado client-side.

## Criacao de abas

```java
TabbedPanel tabs = new TabbedPanel();
tabs.addTab("editor", "Editor.java", new JScrollPane(new JTextArea()));
tabs.addTab("preview", "Preview", new JPanel());
```

| Metodo | Contrato |
|---|---|
| `addTab(String title, Component component)` | Adiciona com chave gerada e retorna a chave |
| `addTab(TabConfig config)` | Adiciona por configuracao |
| `addTab(String key, String title, Component component)` | Adiciona com chave explicita |
| `removeTab(String key)` | Remove direto |
| `closeTab(String key)` | Fecha respeitando regras de fechamento |
| `closeCurrentTab()` | Fecha a aba atual |
| `closeAllTabs()` | Fecha todas que podem fechar |
| `closeOtherTabs(String key)` | Fecha as outras abas |

Use `close...` para fluxo de usuario; use `remove...` para manipulacao direta de modelo.

## Navegacao e consulta

| Metodo | Uso |
|---|---|
| `switchTo(String key)` | Seleciona uma aba |
| `switchFirst()` / `switchLast()` | Primeira/ultima |
| `switchNext()` / `switchPrevious()` | Navegacao sequencial |
| `getCurrentKey()` | Chave atual |
| `getEntry(key)` | Entrada da aba |
| `contains(key)` | Existencia |

## Estado visual

```java
tabs.setDirty("editor", true);
tabs.setBadge("editor", "3");
tabs.setPinned("preview", true);
tabs.setTabTitleForeground("editor", Color.RED);
```

Recursos suportados:

- `closable`
- `pinned`
- `dirty`
- `badge`
- `tooltip`
- icone normal e selecionado
- header customizado
- menu de aba
- botao de nova aba
- lista de abas
- fechar com botao do meio
- renomear com duplo clique

## Eventos

Use helpers fluentes:

```java
tabs.onBeforeTabClose(event -> {
    if (isUnsaved(event.getKey())) {
        event.cancel();
    }
});

tabs.onTabClose(event -> System.out.println("Fechou " + event.getKey()));
```

Ou use `addEventListner` com constantes de `EventTabbedPanel`.

## Drag, reorder e split

```java
tabs.setScrollableTabsEnabled(true)
    .setDockModeEnabled(true)
    .setSplitDropZoneSize(130)
    .setReorderTabsWhileDragging(true);
```

APIs relacionadas: `dockTab`, `splitTab`, `transferTabTo`, `reattachTabTo`, `reattachAllTabs`, `dockAllTabs`, `mergeDockGroups`, `getDockGroups`.

## Exemplo completo

```java
TabbedPanel tabs = new TabbedPanel(JTabbedPane.TOP);

tabs.setDefaultTabMenuEnabled(true)
    .setCloseOnMiddleClickEnabled(true)
    .setNewTabButtonVisible(true)
    .setNewTabAction(() -> {
        String key = "untitled-" + System.nanoTime();
        tabs.addTab(key, "Sem titulo", new JScrollPane(new JTextArea()));
        tabs.switchTo(key);
    });

tabs.addTab("readme", "README.md", new JScrollPane(new JTextArea("# README")));
tabs.addTab("log", "Log", new JScrollPane(new JTextArea()));
tabs.setPinned("log", true);
```

## Tipos auxiliares

`TabConfig`, `TabEntry`, `TabEvent`, `EventTabbedPanel`, `TabStyle`, `TabSplitPlacement`, `TabHeaderFactory`, `DefaultTabHeaderRenderer`, `TabMenuProvider`, `DefaultTabMenuFactory`, `TabGroupFactory`, `TabSeparatorFactory`, `StyledTabbedPaneUI`, `TabDragController`, `TabDragSession`.

## Cuidados

- Chaves devem ser estaveis; evite usar titulo como chave se o titulo pode mudar.
- Nao remova componentes manualmente do `JTabbedPane` interno; use a API do `TabbedPanel`.
- Antes de fechar uma aba com dados nao salvos, use evento `before close` ou `setCloseConfirmationProvider`.
