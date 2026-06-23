# DockPanel

`DockPanel` organiza componentes em regioes encaixaveis, parecido com uma IDE.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.dock` |
| Heranca | `DockPanel extends PanelEventListener` |
| Unidade | `DockEntry`, identificada por `key` |
| Regioes | `DockRegion` |
| Uso principal | Layout com centro, laterais, rodape, paineis moviveis e grupos de abas |

## Heranca

```text
ViewPanel
  BlockingPanel
    PanelEventListener
      DockPanel
```

`DockPanel` herda eventos, bloqueio de UI, DOM local e client state.

## Regioes

`DockRegion` representa o destino de um dock. O centro e o uso padrao para conteudo principal; laterais e rodape sao apropriados para explorador, propriedades, console e resultados.

## Adicionando docks

```java
DockPanel dock = new DockPanel();
dock.addDock("editor", "Editor", editorComponent, DockRegion.CENTER);
dock.addDock("explorer", "Explorer", explorerComponent, DockRegion.LEFT);
dock.addDock("console", "Console", consoleComponent, DockRegion.BOTTOM, new Dimension(1, 240));
```

| Metodo | Contrato |
|---|---|
| `addDock(String title, Component component)` | Adiciona na regiao padrao |
| `addDock(String title, Component component, Dimension preferredSize)` | Adiciona na regiao padrao com tamanho preferido do dock |
| `addDock(String title, Component component, DockRegion region)` | Adiciona na regiao informada |
| `addDock(String title, Component component, DockRegion region, Dimension preferredSize)` | Adiciona na regiao informada com tamanho preferido do dock |
| `addDock(String key, String title, Component component, DockRegion region)` | Adiciona com chave explicita |
| `addDock(String key, String title, Component component, DockRegion region, Dimension preferredSize)` | Adiciona com chave explicita e tamanho preferido do dock |
| `addDock(DockConfig config)` | Adiciona usando configuracao completa |
| `removeDock(String key)` | Remove direto |
| `closeDock(String key)` | Fecha respeitando regras |
| `moveDock(String key, DockRegion region)` | Move para outra regiao |
| `setRegionPreferredSize(DockRegion region, Dimension size)` | Define o tamanho preferido padrao da regiao |
| `setRegionPreferredSize(String key, Dimension size)` | Define por chave o tamanho preferido do dock, igual ao `preferredSize` do `addDock` |

## DockConfig

Use `DockConfig` quando precisar configurar titulo, componente, regiao, icone, tooltip, `closable`, `pinned`, `dirty`, badge e comportamento de drag.

```java
DockConfig config = new DockConfig(
        "console",
        "Console",
        new JScrollPane(console)
).region(DockRegion.BOTTOM)
 .preferredSize(new Dimension(1, 240))
 .closable(true);

dock.addDock(config);
```

O tamanho preferido configurado no dock tem precedencia sobre `setRegionPreferredSize`.
Quando a entrada ativa da regiao nao define tamanho, `DockPanel` usa o tamanho preferido definido/capturado da regiao; se a regiao ainda estiver no tamanho padrao, usa o `preferredSize` do componente como base.
Use `setRegionPreferredSize(String key, Dimension size)` para alterar depois da criacao o mesmo tamanho preferido configurado no `addDock`.

## Eventos

Eventos sao emitidos como `DockEvent`, com constantes em `EventDockPanel`.

```java
dock.addEventListner(EventDockPanel.DOCK_MOVE, event -> {
    DockEvent dockEvent = event.tryGetValue();
    System.out.println(dockEvent.getKey());
});
```

## Extensao

| Tipo | Uso |
|---|---|
| `DockDragPolicy` | Validar se um drag/drop pode ocorrer |
| `DockGroupFactory` | Criar o `TabbedPanel` usado por uma regiao |
| `DockSeparatorFactory` | Customizar divisores/splits |
| `DockDropContext` | Contexto de drop para decisoes |
| `DockLayoutSnapshot` | Salvar/restaurar layout |

## Exemplo de uso

```java
DockPanel dock = new DockPanel();

dock.addDock("editor", "Editor.java", new JScrollPane(new JTextArea()), DockRegion.CENTER);
dock.addDock("project", "Projeto", new JScrollPane(new JTree()), DockRegion.LEFT);
dock.addDock("terminal", "Terminal", new JScrollPane(new JTextArea()), DockRegion.BOTTOM);

dock.moveDock("terminal", DockRegion.RIGHT);
```

## Cuidados

- Use chaves estaveis para persistir/restaurar layout.
- Use `closeDock` para interacao do usuario; ele permite respeitar eventos e regras.
- Se o componente precisa de abas avancadas dentro da regiao, customize a criacao de grupo via `DockGroupFactory`.
