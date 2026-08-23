# RatingField

`RatingField` e um campo de avaliacao por estrelas, com suporte a meia estrela, previa no hover e modo somente leitura.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.ratingfield` |
| Heranca | `RatingField extends PanelEventListener` |
| Uso principal | Nota de 0 a N |

```java
RatingField nota = new RatingField(5, 3.5);
nota.setAllowHalf(true).setIconSize(24);
nota.addEventListener(EventType.CHANGE, e -> salvar((double) e.getValue()));
```

## Valor

| Metodo | Contrato |
|---|---|
| `getValue()` / `setValue(double)` | Le e define, disparando eventos |
| `setValue(double, boolean fireEvent)` | Define controlando o disparo |
| `setCount(int)` / `getCount()` | Quantidade de estrelas |
| `setAllowHalf(boolean)` | Permite incrementos de 0,5 |
| `setClearable(boolean)` | Clicar na estrela ja marcada zera a nota |
| `setReadOnly(boolean)` | Impede alteracao pelo usuario, mantendo a pintura normal |

O valor e limitado a `[0, count]` e alinhado ao incremento corrente.

## Eventos

| Evento | Quando |
|---|---|
| `EventType.CHANGE` | Nota mudou |
| `RatingField.RATED` | Nota mudou |

## Visual

| Metodo | Uso |
|---|---|
| `setIconSize(int)` / `setIconGap(int)` | Geometria |
| `setColors(Color filled, Color empty, Color hover)` | Cores das estrelas |
| `setFocusPainted(boolean)` / `setFocusColor(Color)` | Anel de foco |

Passar o mouse mostra a previa da nota sem alterar o valor. `paintStar` e `getStarBounds` sao `protected`.

## Teclado

Setas incrementam ou decrementam; `Home` zera e `End` marca a nota maxima.
