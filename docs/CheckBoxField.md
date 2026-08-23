# CheckBoxField

`CheckBoxField` e uma caixa de selecao desenhada manualmente, com marcacao animada, estado indeterminado e rotulo opcional.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.checkfield` |
| Heranca | `CheckBoxField extends PanelEventListener` |
| Uso principal | Opcao booleana com estado parcial |

```java
CheckBoxField termos = new CheckBoxField("Aceito os termos");
termos.setSelected(true);
termos.addEventListener(EventType.CHANGE, e -> System.out.println(e.getValue()));
```

## Estado

| Metodo | Contrato |
|---|---|
| `toggle()` | Inverte o estado |
| `isSelected()` / `setSelected(boolean)` | Le e define, disparando eventos |
| `setSelected(boolean, boolean fireEvent)` | Define controlando o disparo |
| `isIndeterminate()` / `setIndeterminate(boolean)` | Estado parcial, desenhado como um traco |

Entrar em indeterminado zera `selected`; marcar ou desmarcar sai do indeterminado.

## Eventos

| Evento | Quando |
|---|---|
| `EventType.CHANGE` | Estado mudou |
| `CheckBoxField.CHECKED` | Passou a marcado |
| `CheckBoxField.UNCHECKED` | Passou a desmarcado |

O evento traz `oldValue` e `newValue`; `firePropertyChange("selected", ...)` tambem e emitido.

## Visual

| Metodo | Uso |
|---|---|
| `setText(String)` | Rotulo ao lado da caixa |
| `setAnimated(boolean)` / `setAnimationDuration(int)` | Animacao do traco de marcacao |
| `setBoxSize(int)` / `setBoxArc(int)` / `setTextGap(int)` | Geometria |
| `setColors(Color selected, Color unselected, Color check)` | Cores principais |
| `setBorderColor(Color)` / `setTextColor(Color)` | Cores auxiliares |
| `setFocusPainted(boolean)` / `setFocusColor(Color)` | Anel de foco |

Sem cores explicitas, o componente le [UiTokens](UiTokens.md). Os submetodos `paintBox`, `paintMark`, `paintText`, `paintFocus` e `getBoxBounds` sao `protected` e podem ser sobrescritos.

## Teclado

`Espaco` e `Enter` alternam o estado quando o componente tem foco.
