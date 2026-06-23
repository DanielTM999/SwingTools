# PanelEventListener

`PanelEventListener` e a base para paineis Swing que precisam emitir eventos do SwingTools.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.base` |
| Heranca | `PanelEventListener extends BlockingPanel implements EventListenerComponent` |
| Uso principal | Criar componentes visuais com bloqueio de UI e eventos padronizados |

## Heranca

```text
ViewPanel
  BlockingPanel
    PanelEventListener
      KeyPanel
      TabbedPanel
      DockPanel
      SwitchField
```

## API publica

| Metodo | Contrato |
|---|---|
| `addEventListner(String, Consumer<EventComponent>)` | Registra listener por tipo |
| `removeEventListner(String, Consumer<EventComponent>)` | Remove listener especifico |
| `removeEventListner(String)` | Remove todos de um tipo |
| `removeAllListeners()` | Remove todos os listeners |
| `getEventListners()` | Retorna copia do mapa de listeners |

## Eventos internos

`PanelEventListener` dispara `EventType.LOAD` em `addNotify()`, depois que o componente entra na hierarquia Swing.

Para subclasses, existem helpers protegidos `dispachEvent(...)` que montam um `EventComponent` com componente emissor, valor e propriedades extras.

```java
public class CounterPanel extends PanelEventListener {
    private int value;

    public void increment() {
        value++;
        dispachEvent(EventType.CHANGE, value, Map.of("source", "increment"));
    }
}
```

## Quando estender

- Quando o componente e visual e precisa emitir eventos de dominio.
- Quando ele tambem precisa bloquear interacao com `lockUI`.
- Quando voce quer manter a API de eventos igual aos componentes existentes.

Se o componente for um campo de texto, prefira herdar de `JTextFieldListener`. Se for uma tabela, prefira `DataTableListener`.
