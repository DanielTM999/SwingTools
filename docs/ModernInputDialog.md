# ModernInputDialog

`ModernInputDialog` e um dialog de entrada com modernDialogBuilder, validacao e submit customizado.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.popup` |
| API | `ModernInputDialog.modernDialogBuilder()` |
| Retorno | Texto confirmado ou `null` quando cancelado |

## Exemplo

```java
String value = ModernInputDialog.modernDialogBuilder()
        .title("Novo arquivo")
        .message("Informe o nome do arquivo.")
        .confirmText("Criar")
        .cancelText("Cancelar")
        .onValidate(context -> {
            if (context.value() == null || context.value().isBlank()) {
                throw new IllegalArgumentException("Informe o nome.");
            }
        })
        .parent(frame)
        .show();
```

## Builder

| Metodo | Uso |
|---|---|
| `title(String)` / `message(String)` | Textos |
| `input(JComponent)` | Componente customizado |
| `parent(Component)` / `parentComponent(Component)` | Janela/componente de referencia para centralizacao |
| `confirmText(String)` / `cancelText(String)` | Labels dos botoes |
| `onValidate(ValidationHandler)` | Validacao |
| `validationDelayMs(int)` | Delay da validacao |
| `disableConfirmWhenInvalid(boolean)` | Bloqueia confirmar se invalido |
| `onSubmit(SubmitHandler)` | Acao de submit |
| `closeOnSubmitSuccess(boolean)` | Fecha apos submit sem erro |
| `closeOnEsc(boolean)` | Fecha ao pressionar `Esc` (padrao: `true`) |
| `show()` / `show(Component)` | Exibe |

## Contratos

`ValidationContext` fornece o valor atual para validacao. `SubmitContext` fornece valor e contexto no submit. Lance `IllegalArgumentException` para mostrar erro ao usuario.
