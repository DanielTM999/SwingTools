# RadioGroupField e RadioField

`RadioField<T>` e um botao de opcao desenhado manualmente. `RadioGroupField<T>` agrupa varios deles, garante selecao unica e expoe o valor escolhido de forma tipada, sem `ButtonGroup`.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.checkfield` |
| Heranca | `RadioField<T> extends PanelEventListener`, `RadioGroupField<T> extends PanelEventListener` |
| Uso principal | Escolha unica entre poucas opcoes |

```java
RadioGroupField<String> plano = new RadioGroupField<>();
plano.addOption("Mensal", "MONTHLY")
     .addOption("Anual", "YEARLY");
plano.setSelectedValue("MONTHLY", false);

plano.addEventListener(EventType.CHANGE, e -> {
    String escolhido = e.tryGetValue();
});
```

## RadioGroupField

| Metodo | Contrato |
|---|---|
| `addOption(String label, T value)` | Cria e adiciona uma opcao |
| `addOption(RadioField<T>)` | Adiciona uma opcao ja construida |
| `setOptions(List<T>, Function<T,String>)` | Recria as opcoes a partir dos valores |
| `setOptions(LinkedHashMap<String,T>)` | Recria a partir de rotulo e valor |
| `clearOptions()` | Remove todas as opcoes |
| `getSelectedValue()` / `getSelectedOption()` | Valor e opcao marcados, ou `null` |
| `setSelectedValue(T)` / `setSelectedValue(T, boolean fireEvent)` | Marca pelo valor |
| `clearSelection(boolean fireEvent)` | Desmarca tudo |
| `setOrientation(Orientation)` | `HORIZONTAL` ou `VERTICAL` |
| `getOptions()` | Copia imutavel das opcoes |

Eventos do grupo: `EventType.CHANGE` e `EventType.SELECT` com o valor escolhido; `EventType.CLEAR` ao desmarcar tudo. As propriedades trazem `oldValue` e `newValue`.

## RadioField

| Metodo | Uso |
|---|---|
| `setValue(T)` / `getValue()` | Valor associado |
| `setText(String)` | Rotulo |
| `setSelected(boolean)` / `setSelected(boolean, boolean fireEvent)` | Marcacao |
| `setAnimated(boolean)` / `setAnimationDuration(int)` | Animacao do ponto central |
| `setCircleSize(int)` / `setTextGap(int)` | Geometria |
| `setColors(Color selected, Color border, Color dot)` | Cores principais |
| `setTextColor(Color)` / `setFocusPainted(boolean)` / `setFocusColor(Color)` | Cores auxiliares |

Eventos da opcao: `EventType.CHANGE`, `EventType.SELECT` e `RadioField.SELECTED`.

`Espaco` e `Enter` marcam a opcao com foco. As cores nao definidas vem de [UiTokens](UiTokens.md).
