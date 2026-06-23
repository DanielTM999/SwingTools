# SwitchField

`SwitchField` e um toggle booleano visual, com suporte a teclado, mouse, animacao e eventos.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.switchfield` |
| Heranca | `SwitchField extends PanelEventListener` |
| Uso principal | Ligar/desligar uma opcao |

## Estado

```java
SwitchField active = new SwitchField();
active.setSelected(true);
active.toggle();
```

| Metodo | Contrato |
|---|---|
| `toggle()` | Inverte estado |
| `isSelected()` | Retorna estado atual |
| `setSelected(boolean)` | Define estado e dispara eventos |
| `setSelected(boolean, boolean fireEvent)` | Define controlando disparo |

## Eventos

| Evento | Quando |
|---|---|
| `EventType.CHANGE` | Estado mudou |
| `EventType.SELECT` | Estado mudou para `true` |
| `SwitchField.SWITCH_ON` | Ligou |
| `SwitchField.SWITCH_OFF` | Desligou |

O evento inclui propriedades `oldValue` e `newValue`.

```java
active.addEventListner(EventType.CHANGE, event -> {
    boolean selected = event.tryGetValue();
    boolean oldValue = (boolean) event.getProperties().get("oldValue");
});
```

## Visual

| Metodo | Uso |
|---|---|
| `setAnimated(boolean)` | Liga/desliga animacao |
| `setAnimationDuration(int)` | Duracao em ms |
| `setShowText(boolean)` | Mostra texto interno |
| `setTexts(String on, String off)` | Textos |
| `setColors(Color on, Color off, Color thumb)` | Cores principais |
| `setDisabledColor(Color)` | Cor disabled |
| `setFocusColor(Color)` | Cor do foco |
| `setTextColor(Color)` | Cor do texto |

## Acessibilidade basica

O componente e focavel. Espaco e Enter alternam o estado quando habilitado.

## Cuidados

- Use `setSelected(value, false)` ao sincronizar estado inicial sem disparar evento.
- Se desabilitado com `setEnabled(false)`, o cursor e a pintura mudam para modo disabled.
