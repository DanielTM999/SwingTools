# TextAreaField

`TextAreaField` e uma area de texto moderna com placeholder, contador de caracteres, limite opcional e crescimento automatico.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.textarea` |
| Heranca | `TextAreaField extends PanelEventListener` |
| Uso principal | Texto longo em formularios |

```java
TextAreaField descricao = new TextAreaField("Descreva o chamado...");
descricao.setMaxLength(280).setAutoGrow(true).setRowRange(3, 10);
```

## Texto

| Metodo | Contrato |
|---|---|
| `getText()` / `setText(String)` | Le e define, disparando eventos |
| `setText(String, boolean fireEvent)` | Define controlando o disparo |
| `setPlaceholder(String)` / `getPlaceholder()` | Texto exibido quando vazio e sem foco |
| `setMaxLength(int)` / `getMaxLength()` | Limite; negativo remove o limite |
| `getTextArea()` | Acesso ao `JTextArea` interno |

O limite trunca a entrada em vez de rejeitar o bloco inteiro: colar um texto maior que o limite preenche ate o maximo e dispara `LIMIT_REACHED`.

## Layout

| Metodo | Uso |
|---|---|
| `setShowCounter(boolean)` | Contador `120/500` abaixo do campo |
| `setAutoGrow(boolean)` | Cresce conforme as linhas digitadas |
| `setRowRange(int min, int max)` | Faixa de linhas do crescimento automatico |

O contador fica vermelho ao atingir o limite.

## Estado e cores

| Metodo | Uso |
|---|---|
| `setErrorState(boolean)` / `isErrorState()` | Borda de erro; usado pelo [FormField](FormPanel.md) |
| `setArc(int)` | Raio de canto |
| `setColors(Color background, Color border, Color focusBorder)` | Cores principais |
| `setPlaceholderColor(Color)` | Cor do placeholder |

## Eventos

| Evento | Quando |
|---|---|
| `EventType.INPUT` | Texto mudou, com `length` nas propriedades |
| `EventType.CHANGE` | `setText` foi chamado |
| `EventType.FOCUS` / `EventType.BLUR` | Foco entrou ou saiu |
| `TextAreaField.LIMIT_REACHED` | Entrada foi truncada pelo limite |
