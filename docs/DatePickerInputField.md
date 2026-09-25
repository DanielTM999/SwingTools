# DatePickerInputField

`DatePickerInputField` e um input de data/hora com calendario, texto formatado e eventos.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.datefield` |
| Heranca | `DatePickerInputField extends PanelEventListener implements DatePickerField` |
| Valor | `LocalDateTime` / `LocalDate` |

## Criacao

```java
import dtm.stools.component.panels.datefield.DatePickerInputField;

import java.time.LocalDate;

DatePickerInputField date = new DatePickerInputField("dd/MM/yyyy");
date.setSelectedDate(LocalDate.now());
LocalDate selected = date.getSelectedDate();
```

Crie e adicione o campo na EDT. `getSelectedDate()` serve para formulários apenas com dia; use `getSelectedDateTime()` quando hora e minuto fazem parte do valor. O formato passado ao construtor governa a apresentação textual.

## API

| Metodo | Uso |
|---|---|
| `getSelectedDateTime()` / `setSelectedDateTime(LocalDateTime)` | Valor completo |
| `getSelectedDate()` / `setSelectedDate(LocalDate)` | Somente data |
| `getFormattedText()` | Texto formatado |
| `clear()` | Limpa valor |
| `setEditable(boolean)` | Permite edicao |
| `setReadonlyField(boolean)` / `isReadonlyField()` | Readonly visual |
| `getComponent()` | Retorna componente Swing |

## Eventos

Como herda `PanelEventListener`, aceita `addEventListener`. Use `EventType.CHANGE` para reagir a alteracao de data.

## Cuidados

- O formato informado deve ser compativel com o parser/formatter interno.
- Diferencie `setEditable(false)` de `setReadonlyField(true)` conforme o comportamento visual desejado.
