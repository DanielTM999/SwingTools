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

## Mensagem extensa com limite de tela

O limite ja vem ativado. Portanto, basta informar a mensagem longa; quando necessario,
o conteudo recebe rolagem vertical e o cabecalho e os botoes continuam visiveis.

```java
String longMessage = ("Uma linha extensa da mensagem.<br>").repeat(200);

int result = ModernDialog.builder()
        .title("Relatorio")
        .message(longMessage)
        .type(ModernDialog.Type.INFO)
        .limitToScreen(true) // opcional: true ja e o padrao
        .option("Fechar", JOptionPane.CLOSED_OPTION)
        .parent(frame)
        .show();
```

A mesma opcao esta disponivel na fachada `Dialogs`:

```java
int result = Dialogs.builder()
        .title("Detalhes")
        .message(longMessage)
        .limitToScreen(true)
        .parent(frame)
        .show();
```

## Desativando o limite

Use apenas quando o tamanho natural do dialog for desejado, mesmo que possa ultrapassar a tela:

```java
ModernDialog.builder()
        .title("Conteudo sem limite")
        .message(longMessage)
        .limitToScreen(false)
        .parent(frame)
        .show();
```

## Exemplo executavel

Veja [ModernDialogScreenLimitExample.java](../src/test/java/dtm/stools/examples/ModernDialogScreenLimitExample.java)
para alternar a flag em tempo de execucao e comparar as APIs `ModernDialog` e `Dialogs`.

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
| `limitToScreen(boolean)` | Impede que o dialog ultrapasse a area util da tela e adiciona rolagem a mensagens extensas (padrao: `true`) |
| `show()` / `show(Component)` | Exibe e retorna valor |

## Cuidados

- Use constantes de `JOptionPane` ou codigos proprios como retorno.
- Use `parent(frame).show()` ou `show(parent)` para centralizar em relacao a janela correta.
- Para permitir que o dialog use seu tamanho natural mesmo quando ultrapassar a tela, configure `limitToScreen(false)`.
