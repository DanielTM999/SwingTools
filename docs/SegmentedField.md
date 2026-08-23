# SegmentedField

`SegmentedField<T>` e um controle segmentado em formato de pilula, com indicador deslizante animado sobre o segmento marcado.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.segmentedfield` |
| Heranca | `SegmentedField<T> extends PanelEventListener` |
| Uso principal | Alternar entre 2 a 5 modos de visualizacao |

```java
SegmentedField<String> periodo = new SegmentedField<>();
periodo.addSegment("Dia", "DAY")
       .addSegment("Semana", "WEEK")
       .addSegment("Mes", "MONTH");
periodo.setSelectedValue("WEEK", false);
```

## Segmentos

| Metodo | Contrato |
|---|---|
| `addSegment(String label, T value)` | Adiciona ao fim; o primeiro vira o selecionado |
| `setSegments(List<T>, Function<T,String>)` | Recria a partir dos valores |
| `setSegments(LinkedHashMap<String,T>)` | Recria a partir de rotulo e valor |
| `getSegments()` | Copia imutavel dos segmentos |

Cada segmento e o record `SegmentedField.Segment<T>(String label, T value)`.

## Selecao

| Metodo | Contrato |
|---|---|
| `getSelectedIndex()` / `getSelectedValue()` | Estado corrente |
| `setSelectedIndex(int)` / `setSelectedIndex(int, boolean fireEvent)` | Marca pelo indice |
| `setSelectedValue(T)` / `setSelectedValue(T, boolean fireEvent)` | Marca pelo valor |

Eventos: `EventType.CHANGE`, `EventType.SELECT` e `SegmentedField.SEGMENT_SELECTED`, com `index` nas propriedades.

## Visual

| Metodo | Uso |
|---|---|
| `setAnimated(boolean)` / `setAnimationDuration(int)` | Deslize do indicador |
| `setArc(int)` | Raio do trilho e do indicador |
| `setSegmentPadding(int)` / `setMinSegmentWidth(int)` / `setPreferredHeight(int)` | Geometria |
| `setColors(Color track, Color indicator, Color selectedText, Color text)` | Cores principais |
| `setFocusPainted(boolean)` / `setFocusColor(Color)` | Anel de foco |

## Teclado

`Esquerda` e `Direita` movem um segmento; `Home` e `End` vao para as extremidades.
