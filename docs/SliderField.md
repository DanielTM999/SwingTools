# SliderField

`SliderField` e um controle deslizante de valor unico, com trilho fino, polegar arredondado, marcacoes e balao de valor opcionais.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.sliderfield` |
| Heranca | `SliderField extends PanelEventListener` |
| Uso principal | Escolher um numero dentro de um intervalo |

```java
SliderField volume = new SliderField(0, 10, 6);
volume.setStep(1).setShowValue(true).setShowTicks(true).setTickCount(11);
volume.setValueFormatter(v -> Math.round(v) + " / 10");
```

## Valor

| Metodo | Contrato |
|---|---|
| `getValue()` / `setValue(double)` | Le e define, disparando eventos |
| `setValue(double, boolean fireEvent)` | Define controlando o disparo |
| `setRange(double min, double max)` | Intervalo; `min` deve ser menor que `max` |
| `getMinimum()` / `getMaximum()` | Limites correntes |
| `setStep(double)` | Incremento; zero aceita valores continuos |

O valor e sempre limitado ao intervalo e alinhado ao passo antes de ser aplicado.

## Eventos

| Evento | Quando |
|---|---|
| `EventType.CHANGE` | Valor mudou |
| `SliderField.VALUE_CHANGED` | Valor mudou |
| `SliderField.DRAG_FINISHED` | O usuario soltou o polegar |

`DRAG_FINISHED` e o gancho certo para persistir o valor sem gravar a cada pixel arrastado.

## Visual

| Metodo | Uso |
|---|---|
| `setShowValue(boolean)` / `setValueFormatter(DoubleFunction<String>)` | Balao de valor |
| `setShowTicks(boolean)` / `setTickCount(int)` | Marcacoes do trilho |
| `setTrackHeight(int)` / `setThumbSize(int)` / `setPreferredWidth(int)` | Geometria |
| `setColors(Color track, Color fill, Color thumb)` | Cores principais |
| `setValueColor(Color)` / `setFocusPainted(boolean)` / `setFocusColor(Color)` | Cores auxiliares |

## Teclado

`Esquerda`/`Baixo` e `Direita`/`Cima` movem um passo; `PageUp` e `PageDown` movem dez passos; `Home` e `End` vao para os limites.
