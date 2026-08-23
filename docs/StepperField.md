# StepperField

`StepperField` e um campo numerico com botoes de menos e mais, repeticao automatica ao manter pressionado e suporte a roda do mouse.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.stepperfield` |
| Heranca | `StepperField extends PanelEventListener` |
| Uso principal | Quantidades pequenas com ajuste fino |

Internamente ele reusa o [NumberField](NumberField.md), entao herda `BigDecimal`, locale, casas decimais e limites, sem duplicar a logica numerica.

```java
StepperField quantidade = new StepperField(BigDecimal.ONE);
quantidade.setRange(BigDecimal.ZERO, BigDecimal.TEN)
          .setStep(BigDecimal.ONE)
          .setDecimalPlaces(0);
```

## Valor

| Metodo | Contrato |
|---|---|
| `getValue()` / `setValue(BigDecimal)` | Le e define, disparando eventos |
| `setValue(BigDecimal, boolean fireEvent)` | Define controlando o disparo |
| `setRange(BigDecimal min, BigDecimal max)` | Limites; os botoes desabilitam nas pontas |
| `setStep(BigDecimal)` / `getStep()` | Incremento dos botoes |
| `setDecimalPlaces(int)` | Casas decimais exibidas |
| `increment()` / `decrement()` | Aplica um passo por codigo |
| `getNumberField()` | Acesso ao `NumberField` interno |

O valor sempre respeita os limites: incrementar acima do maximo grampeia no maximo em vez de falhar.

## Eventos

| Evento | Quando |
|---|---|
| `EventType.CHANGE` | Valor mudou |
| `StepperField.INCREMENTED` | Valor subiu |
| `StepperField.DECREMENTED` | Valor desceu |

## Visual e interacao

| Metodo | Uso |
|---|---|
| `setWheelEnabled(boolean)` | Roda do mouse altera o valor |
| `setButtonWidth(int)` / `setArc(int)` | Geometria |
| `setColors(Color background, Color border)` | Cores principais |

Manter um botao pressionado repete o passo apos 400 ms, a cada 70 ms.
