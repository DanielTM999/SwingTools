# PinField

`PinField` e o campo de codigo de verificacao: uma caixa por digito, avanco automatico e colagem distribuida entre as caixas.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.pinfield` |
| Heranca | `PinField extends PanelEventListener` |
| Uso principal | OTP, PIN e codigos curtos |

```java
import dtm.stools.component.inputfields.pinfield.PinField;

PinField codigo = new PinField(6);
codigo.addEventListener(PinField.COMPLETED, e -> {
    String pin = codigo.getValue();
    System.out.println("Código completo: " + pin);
});
```

Use `isComplete()` antes de enviar o PIN para sua rotina de validação. `COMPLETED` informa que todas as caixas foram preenchidas; a decisão de aceitar o código pertence à aplicação. `setMasked(true)` oculta a representação visual, mas `getValue()` continua devolvendo o texto.

## Valor

| Metodo | Contrato |
|---|---|
| `getValue()` | Texto digitado, sem as posicoes vazias |
| `setValue(String)` / `setValue(String, boolean fireEvent)` | Preenche as caixas com os caracteres aceitos |
| `isComplete()` | Todas as caixas preenchidas |
| `clear()` | Limpa e dispara `EventType.CLEAR` |
| `setLength(int)` / `getLength()` | Quantidade de caixas; trocar descarta o valor |
| `setNumericOnly(boolean)` | Restringe a digitos; ligado por padrao |
| `setMasked(boolean)` / `isMasked()` | Exibe pontos no lugar dos caracteres |

Caracteres rejeitados sao ignorados, inclusive na colagem: `setValue("12ab34")` em um campo numerico de 4 caixas resulta em `1234`.

## Eventos

| Evento | Quando |
|---|---|
| `EventType.INPUT` | Qualquer alteracao do texto |
| `EventType.CHANGE` | Qualquer alteracao do texto |
| `EventType.CLEAR` | `clear()` foi chamado |
| `PinField.COMPLETED` | A ultima caixa foi preenchida |
| `EventType.SUBMIT` | Emitido junto com `COMPLETED` |

## Visual

| Metodo | Uso |
|---|---|
| `setBoxSize(int width, int height)` / `setBoxGap(int)` / `setBoxArc(int)` | Geometria |
| `setColors(Color box, Color border, Color activeBorder, Color text)` | Cores principais |
| `setFocusPainted(boolean)` | Realce da caixa ativa |

## Teclado e mouse

`Backspace` e `Delete` apagam, as setas navegam entre caixas, `Ctrl+V` cola distribuindo os caracteres e o clique posiciona o cursor na caixa escolhida.
