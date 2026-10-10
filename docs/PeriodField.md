# PeriodField

Campo independente em `dtm.stools.component.inputfields.periodfield` para períodos locais. O modo é definido na construção e não muda durante a vida do componente.

| Modo | Valor | Formato padrão |
|---|---|---|
| `DATE` | `DateRange(LocalDate start, LocalDate end)` | `dd/MM/uuuu` |
| `DATE_TIME` | `DateTimeRange(LocalDateTime start, LocalDateTime end)` | `dd/MM/uuuu HH:mm` |
| `TIME` | `TimeRange(LocalTime start, LocalTime end, boolean nextDay)` | `HH:mm` |

Os três valores implementam `PeriodValue`. Datas incluem as duas extremidades; horários não pressupõem fuso. Início igual ao fim é válido. Valores de outro modo são rejeitados antes de alterar o campo.

```java
PeriodField periodo = new PeriodField(PeriodMode.DATE_TIME)
    .setPresentation(PeriodField.Presentation.CALENDAR);
periodo.setValue(new DateTimeRange(
    LocalDateTime.of(2026, 10, 10, 9, 0),
    LocalDateTime.of(2026, 10, 12, 18, 0)));

PeriodField turno = new PeriodField(PeriodMode.TIME).setAllowOvernight(true);
turno.setValue(new TimeRange(LocalTime.of(22, 0), LocalTime.of(2, 0), true));

PeriodField horarioObrigatorio = new PeriodField(PeriodMode.TIME).setRequired(true);
```

| API | Contrato |
|---|---|
| `getValue()` / `setValue(PeriodValue, boolean)` | Valor confirmado; `false` suprime eventos da biblioteca |
| `clear(boolean)` | Remove valor, rascunho e erro |
| `setLimits(Temporal minimum, Temporal maximum)` | Limites inclusivos, do mesmo tipo temporal do modo; `null` remove uma extremidade |
| `setFormat(String)` / `setLocale(Locale)` | Formatação e interpretação estrita; padrões incompatíveis são rejeitados |
| `setAllowOvernight(boolean)` | Habilita fim no dia seguinte no modo `TIME`; desabilitado por padrão |
| `setRequired(boolean)` / `isRequired()` | Exige um período; em `TIME` e `DATE_TIME`, Aplicar exige horas e minutos escolhidos nos dois relógios |
| `setPresentation(FIELDS/CALENDAR)` | Em `DATE`, habilita o botão do calendário; `DATE_TIME` sempre oferece calendário e relógios; `TIME` aceita somente `FIELDS` e sempre oferece relógios |
| `showCalendar()` / `showTimePicker()` | Abre a seleção visual; `showTimePicker()` funciona em `TIME` e `DATE_TIME` |
| `isInputValid()` / `getValidationMessage()` | Estado da entrada digitada |
| `setEditable(boolean)` / `setEnabled(boolean)` | Controla interação e fecha o popup ao bloquear o campo |
| `getStartInput()` / `getEndInput()` | Personalização dos controles nativos |

Sem `allowOvernight`, o fim não pode preceder o início. Quando habilitado, `nextDay` deve ser verdadeiro exatamente quando o fim é anterior ao início. Os limites no modo hora restringem os dois horários de relógio; não representam uma janela de limite que atravessa a meia-noite.

O calendário mostra dois meses consecutivos. O primeiro clique escolhe uma data; o segundo escolhe a outra, ordenando início e fim. Somente essas duas datas ficam destacadas; os dias intermediários permanecem sem seleção. **Aplicar** valida e confirma; **Cancelar**, Esc e fechamento externo preservam o valor anterior. `RangeCalendarPanel` também pode ser utilizado independentemente.

Em `TIME`, o popup oferece dois relógios, um para início e outro para fim. Em `DATE_TIME`, as abas **Datas** e **Horários** permitem ajustar as duas datas e os dois relógios antes de aplicar. Os relógios usam 24 horas: escolha a hora nos anéis externo (1–12) e interno (13–23 e 00), depois os minutos. Clique ou arraste no mostrador de minutos para escolher qualquer valor de 00 a 59, incluindo as marcas entre os números de cinco em cinco. Os botões +/− e as setas também ajustam a unidade selecionada em passos de um. Os campos continuam aceitando digitação.

Sem um valor anterior, os relógios começam em `--:--`. Com `setRequired(true)`, selecione horas e minutos de início e fim antes de aplicar; escolhas incompletas mostram uma mensagem e mantêm o popup aberto. Em `DATE_TIME`, Aplicar com horários obrigatórios incompletos abre automaticamente a aba **Horários**. O foco vai para horas ou minutos do primeiro relógio incompleto, preservando as datas escolhidas. `00:00` é válido quando escolhido explicitamente. Um valor definido por `setValue(...)` ou digitado por completo já conta como escolha. Limpar um período obrigatório deixa `isInputValid()` falso e bloqueia o envio do formulário. `FormField.setRequired(...)` propaga a opção para o `PeriodField`. Em campos opcionais, unidades não escolhidas continuam usando zero ao aplicar.

`ClockPickerPanel(Locale)` também pode ser utilizado independentemente com `getTime()`, `setTime(LocalTime)` e `setSelectingHours(boolean)`. `getTime()` retorna `null` antes de qualquer escolha; uma escolha parcial retorna o horário com zero na unidade restante. `setTime(null)` limpa a escolha; `setTime(LocalTime)` confirma ambas as unidades, inclusive meia-noite. `setRequired(true)` e `isSelectionValid()` permitem validar o relógio independente; `isHourSelected()` e `isMinuteSelected()` distinguem escolhas explícitas. Publica as propriedades `time` e `selectionComplete`, preserva segundos e nanos existentes e oferece botões nativos com nomes acessíveis para horas e minutos.

No calendário, setas movem o foco por dia/semana; Page Up/Down por mês; Home/End dentro da semana; Enter ou Espaço selecionam. Cada dia possui nome acessível completo e estado selecionado somente nas duas datas escolhidas. Nos relógios, Tab percorre os controles; Enter ou Espaço escolhem o número e as setas ajustam horas/minutos.

Digitação incompleta ou inválida mantém o último valor válido, apresenta erro e bloqueia envio pelo `FormPanel`, inclusive em campos opcionais. Ambos os campos vazios representam `null`. `CHANGE` publica somente alterações válidas; `INPUT` informa se o rascunho é válido. Alterações silenciosas continuam atualizando os controles e notificações acessíveis.

Exemplo executável: `dtm.stools.examples.PeriodFieldExample`.
