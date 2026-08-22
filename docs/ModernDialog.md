# ModernDialog

`ModernDialog` e uma API modernDialogBuilder para dialogs modais com botoes customizados.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.popup` |
| API | `ModernDialog.modernDialogBuilder()` |
| Tipos | `SUCCESS`, `ERROR`, `INFO`, `QUESTION` |

## Exemplo

```java
int result = ModernDialog.modernDialogBuilder()
        .title("Excluir")
        .message("Deseja excluir o registro?")
        .type(ModernDialog.Type.QUESTION)
        .option("Excluir", JOptionPane.OK_OPTION, new Color(0xDC2626), Color.WHITE)
        .option("Cancelar", JOptionPane.CANCEL_OPTION)
        .parent(frame)
        .show();
```

## Builder

| Metodo | Uso |
|---|---|
| `title(String)` | Titulo |
| `message(String)` | Mensagem |
| `type(Type)` | Tipo visual |
| `typeLabel(String)` | Personaliza o texto do tipo exibido no topo |
| `showTypeLabel(boolean)` | Exibe ou oculta o indicador de tipo no topo (padrao: `true`) |
| `accentColor(Color)` | Cor de destaque |
| `parent(Component)` / `parentComponent(Component)` | Janela/componente de referencia para centralizacao |
| `option(String, int)` | Botao e retorno |
| `option(String, int, Color)` | Botao com background |
| `option(String, int, Color, Color)` | Botao com background e foreground |
| `draggable(boolean)` | Permite arrastar |
| `closeOnEsc(boolean)` | Fecha ao pressionar `Esc` (padrao: `true`) |
| `show()` / `show(Component)` | Exibe e retorna valor |

## Cuidados

- Use constantes de `JOptionPane` ou codigos proprios como retorno.
- Use `parent(frame).show()` ou `show(parent)` para centralizar em relacao a janela correta.
