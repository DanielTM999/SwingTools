# SmartTextArea

`SmartTextArea` é um `JTextArea` com os recursos que faltam no componente padrão. Use-o quando o [CodeEditor](CodeEditor.md) for pesado demais para o caso, como em notas, logs editáveis, templates e configurações simples. Ele adiciona:

- desfazer/refazer agrupado;
- menu de contexto;
- placeholder;
- limite de caracteres;
- numeração de linhas e destaque da linha atual;
- busca e substituição;
- atalhos de edição de linhas;
- redimensionamento pelo canto, como o `<textarea>` da web;
- diagnósticos (sublinhado ondulado), com corretor ortográfico por dicionário.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.textarea` |
| Herança | `SmartTextArea extends JTextArea implements EventListenerComponent` |
| Rolagem | `createScrollPane()` devolve um `SmartTextAreaScrollPane` (`JScrollPane`) |

```java
import dtm.stools.component.inputfields.textarea.SmartTextArea;

SmartTextArea area = new SmartTextArea();
area.setPlaceholder("Escreva aqui...")
    .setMaxLength(5000)
    .setUseSpacesForTab(true);
area.setTabSize(4);

panel.add(area.createScrollPane(), BorderLayout.CENTER);
```

Por ser um `JTextArea`, tudo do componente padrão continua valendo: `setLineWrap`, `setWrapStyleWord`, `setEditable`, `getDocument` etc. Os métodos novos devolvem a própria instância para encadear chamadas.

Numeração de linhas, busca e redimensionamento dependem do `SmartTextAreaScrollPane`. Se o componente for colocado num `JScrollPane` comum, o resto funciona normalmente, mas `Ctrl+F` não faz nada e não há gutter nem alça de redimensionar.

## Desfazer e refazer

| Método | Contrato |
|---|---|
| `undo()` / `redo()` | Desfaz/refaz; ignorado se não editável |
| `canUndo()` / `canRedo()` | Estado da pilha |
| `discardAllEdits()` | Limpa o histórico |
| `beginCompoundEdit()` / `endCompoundEdit()` | Agrupa várias edições num único passo (aceita aninhamento) |
| `getUndoManager()` | Acesso ao `UndoManager` |

Como as edições são agrupadas:

- A digitação é agrupada por palavra. Um espaço inicia um novo grupo, e o grupo também fecha com Enter, com um salto do cursor ou após cerca de 1 s parado.
- `setText`, as substituições e os atalhos de linha viram um único passo.
- O texto passado no construtor não entra no histórico.

## Texto e limite

| Método | Uso |
|---|---|
| `setPlaceholder(String)` / `setPlaceholderColor(Color)` | Texto exibido enquanto o documento está vazio |
| `setMaxLength(int)` | Limite de caracteres; negativo remove |
| `clear()` | Limpa e dispara `CLEAR` |

O limite trunca a entrada, como no [TextAreaField](TextAreaField.md): colar um texto maior preenche até o máximo e dispara `LIMIT_REACHED`. Trocar o documento com `setDocument` mantém o limite e reinicia o histórico.

## Visual

| Método | Uso |
|---|---|
| `setLineNumbersVisible(boolean)` | Gutter com números de linha (padrão `true`) |
| `setHighlightCurrentLine(boolean)` | Faixa na linha do cursor (padrão `true`) |
| `setCurrentLineColor(Color)` | Cor da faixa; `null` segue o tema |

Com `setLineWrap(true)`, o número aparece só na primeira linha visual de cada linha lógica. As cores seguem o `UiTokens` e acompanham a troca de tema.

## Redimensionamento

Como o `resize` do CSS, o canto inferior direito tem uma alça que o usuário arrasta para mudar o tamanho. Vem habilitado (`BOTH`).

| Método | Uso |
|---|---|
| `setResize(TextAreaResize)` / `getResize()` | `BOTH` (padrão), `VERTICAL`, `HORIZONTAL` ou `NONE` |

Comportamento:

- Arrastar altera o `preferredSize` do `SmartTextAreaScrollPane` e chama `revalidate` no pai. O efeito aparece em layouts que respeitam o tamanho preferido, como `FlowLayout`, `BoxLayout` e as bordas `NORTH`/`SOUTH` do `BorderLayout`. No `CENTER` de um `BorderLayout`, o layout ignora o tamanho preferido.
- O mínimo é uma linha de texto. `setMinimumSize`/`setMaximumSize` no scroll pane limitam o arraste.
- Clique duplo na alça volta ao tamanho original.
- Cada mudança dispara `EventType.RESIZE` com a nova `Dimension`.

```java
SmartTextArea comentario = new SmartTextArea(4, 40);
comentario.setResize(TextAreaResize.VERTICAL);
form.add(comentario.createScrollPane());
```

## Diagnósticos

Diagnósticos marcam trechos do texto com sublinhado ondulado, como num editor. Servem para ortografia, validação de formato, palavras proibidas ou qualquer outra regra. Os tipos públicos ficam em `dtm.stools.component.inputfields.textarea.diagnostic`.

| Tipo | Papel |
|---|---|
| `TextDiagnostic` | `record(start, end, severity, message, source, actions)`; atalhos `error`/`warning`/`info`/`hint` e `withSource`, `withActions`, `withRange` |
| `DiagnosticSeverity` | `ERROR` (vermelho), `WARNING` (amarelo), `INFO` (azul), `HINT` (pontilhado discreto) |
| `TextDiagnosticProvider` | `List<TextDiagnostic> diagnose(String text)` |
| `TextDiagnosticAction` | Correção rápida mostrada no menu de contexto; `replaceWith(texto)` ou `of(rótulo, (area, diagnostico) -> ...)` |
| `DictionarySpellChecker` | Provider pronto de ortografia por lista de palavras |

```java
TextDiagnosticProvider semEmail = texto -> {
    List<TextDiagnostic> achados = new ArrayList<>();
    Matcher m = Pattern.compile("[\\w.]+@[\\w.]+").matcher(texto);
    while (m.find()) {
        achados.add(TextDiagnostic.warning(m.start(), m.end(), "Não inclua e-mails neste campo")
                .withSource("política")
                .withActions(TextDiagnosticAction.replaceWith("[e-mail removido]")));
    }
    return achados;
};

area.addDiagnosticProvider(semEmail)
    .addDiagnosticProvider(DictionarySpellChecker.fromFile(Path.of("pt_BR.dic")));
```

### Como os providers rodam

- Rodam fora da EDT, cerca de 400 ms depois da última edição (`setDiagnosticsDelay`), sobre uma cópia do texto. Por isso não devem tocar em componentes Swing.
- Resultados de uma versão antiga do texto são descartados. Se o texto mudou enquanto o provider rodava, vale a próxima execução.
- Uma exceção num provider é registrada e não derruba os outros.
- Enquanto o usuário digita, os sublinhados existentes acompanham o texto até a próxima execução.

### API

| Método | Uso |
|---|---|
| `addDiagnosticProvider(p)` / `removeDiagnosticProvider(p)` | Registra/remove um provider |
| `setDiagnostics(List)` | Diagnósticos manuais (ex.: vindos de uma validação no servidor); convivem com os dos providers |
| `getDiagnostics()` / `getDiagnosticsAt(offset)` | Diagnósticos com os offsets atuais |
| `clearDiagnostics()` | Remove todos até a próxima execução |
| `runDiagnostics()` | Executa agora, sem esperar o atraso |
| `nextDiagnostic()` / `previousDiagnostic()` | Seleciona o próximo/anterior (`F8` / `Shift+F8`) |

### Interface

- O mouse sobre o trecho mostra um tooltip com a severidade, a origem e a mensagem.
- O gutter mostra um ponto colorido nas linhas com erro, aviso ou informação.
- O botão direito sobre o trecho lista o diagnóstico e as ações no topo do menu. Aplicar uma ação remove aquele diagnóstico e entra no desfazer como um passo.
- `DIAGNOSTICS_CHANGED` é disparado sempre que a lista muda.

### Corretor ortográfico

`DictionarySpellChecker` marca palavras fora do dicionário e sugere as mais próximas (até 2 edições, incluindo letras trocadas de lugar). Cada sugestão mantém a capitalização da palavra original. O menu também traz "Adicionar ao dicionário".

| Método | Uso |
|---|---|
| `new DictionarySpellChecker(Collection<String>)` | Dicionário em memória |
| `fromFile(Path)` / `fromStream(InputStream, Charset)` | Uma palavra por linha; aceita `.dic` do Hunspell (ignora `/FLAGS` e a contagem), mas não expande afixos |
| `addWord`, `addWords`, `removeWord`, `isKnown`, `suggest` | Manutenção e consulta |
| `setSeverity`, `setMinWordLength`, `setMaxSuggestions`, `setMaxWordsWithSuggestions`, `setIgnoreUppercase`, `setAddToDictionaryAction` | Ajustes (encadeáveis) |

A comparação ignora maiúsculas/minúsculas. Palavras com hífen ou apóstrofo são aceitas se cada parte existir. Siglas em caixa alta são ignoradas por padrão. A biblioteca não traz dicionário: carregue um arquivo de palavras do idioma desejado.

## Busca e substituição

| Atalho / método | Ação |
|---|---|
| `Ctrl+F` / `openSearch()` | Abre a barra; a seleção de uma linha vira o termo |
| `Ctrl+H` / `openReplace()` | Abre com a linha de substituição |
| `F3` / `Shift+F3`, `findNext()` / `findPrevious()` | Próximo / anterior, com volta ao início |
| `Enter` / `Shift+Enter` no campo | Próximo / anterior |
| `Esc` / `closeSearch()` | Fecha e devolve o foco |
| `setSearchEnabled(boolean)` | Liga/desliga a busca |
| `setSearchMode(SearchPanelMode)` | `FLOATING` (padrão) ou `EMBEDDED` |

Por padrão (`SearchPanelMode.FLOATING`) a barra flutua no canto superior direito, como no VS Code. Com `SearchPanelMode.EMBEDDED` ela ocupa uma faixa no topo do scroll pane, em toda a largura, e o texto desce para dar espaço. O mesmo enum é usado pelo `CodeEditor` (`setSearchPanelMode`). A barra tem estas opções:

- diferenciar maiúsculas;
- palavra inteira;
- regex, com `$1` na substituição.

"Todos" substitui tudo num único passo de desfazer.

## Atalhos de edição

| Atalho | Ação |
|---|---|
| `Ctrl+D` | Duplica a linha (ou as linhas da seleção) |
| `Alt+↑` / `Alt+↓` | Move as linhas mantendo a seleção |
| `Ctrl+Shift+K` | Exclui as linhas |
| `Tab` com seleção em várias linhas | Indenta as linhas |
| `Tab` sem seleção | Insere `\t`, ou espaços até a próxima parada se `setUseSpacesForTab(true)` |
| `Shift+Tab` | Desindenta |
| `Enter` | Mantém a indentação da linha atual (`setAutoIndent`) |

| Método | Uso |
|---|---|
| `setEditShortcutsEnabled(boolean)` | Desliga os atalhos acima; `Tab` volta a inserir `\t` |
| `setAutoIndent(boolean)` | Liga/desliga a indentação automática |
| `setUseSpacesForTab(boolean)` | Espaços no lugar de `\t` |
| `duplicateLines()`, `moveLinesUp()`, `moveLinesDown()`, `deleteLines()`, `indentSelection()`, `outdentSelection()` | As mesmas ações via código |

## Menu de contexto

O botão direito mostra estas opções, habilitadas conforme seleção, edição e área de transferência:

- Desfazer, Refazer;
- Recortar, Copiar, Colar, Excluir;
- Selecionar tudo;
- Buscar, Substituir.

`setContextMenuEnabled(false)` remove o menu. Um `setComponentPopupMenu(...)` próprio tem prioridade sobre o padrão.

## Eventos

| Evento | Quando |
|---|---|
| `EventType.INPUT` | Texto mudou, com `length` nas propriedades |
| `EventType.CHANGE` | Perdeu o foco com o texto diferente de quando ganhou |
| `EventType.FOCUS` / `EventType.BLUR` | Foco entrou ou saiu |
| `EventType.CLEAR` | `clear()` foi chamado |
| `EventType.LOAD` | Componente foi exibido |
| `EventType.RESIZE` | Usuário redimensionou pela alça, com a nova `Dimension` |
| `SmartTextArea.DIAGNOSTICS_CHANGED` | Lista de diagnósticos mudou, com a lista como valor |
| `SmartTextArea.LIMIT_REACHED` | Entrada foi truncada pelo limite, com `maxLength` nas propriedades |

Exemplo executável: `SmartTextAreaExample`.
