# CodeEditor

`CodeEditor` e um editor de codigo Swing extensivel por providers. Ele combina area de texto, buffer, gutter, minimap, busca, markers, snippets e recursos semanticos opcionais.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.editor.code` |
| Heranca | `CodeEditor extends BlockingPanel` |
| Texto | `CodeEditorTextArea` + `TextBuffer` |
| Scroll | `CodeEditorScrollPane` |
| Lateral | `CodeEditorGutter` |

## Heranca e composicao

```text
ViewPanel
  BlockingPanel
    CodeEditor

CodeEditor
  CodeEditorTextArea
  CodeEditorScrollPane
  CodeEditorGutter
  CodeEditorMinimap
```

Por herdar `BlockingPanel`, o editor pode ser bloqueado com `lockUI` durante analises, formatacao ou carregamento.

## Criacao basica

```java
import dtm.stools.component.panels.editor.code.CodeEditor;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Font;

CodeEditor editor = new CodeEditor("""
        public class Main {
            public static void main(String[] args) {
                System.out.println("Hello");
            }
        }
        """);

editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
frame.add(editor, BorderLayout.CENTER);
```

## API principal

| Metodo | Uso |
|---|---|
| `getText()` | Texto completo |
| `setText(String)` | Troca texto |
| `setReadOnly(boolean)` | Bloqueia edicao pelo usuario |
| `isReadOnly()` | Indica se o editor esta somente leitura |
| `getBuffer()` | Buffer interno |
| `getTextArea()` | Area de texto |
| `getGutter()` | Gutter |
| `getMinimap()` | Minimap |
| `addProvider(CodeEditorProvider)` | Registra extensao |
| `addGutterLayer(GutterLayer)` | Adiciona camada no gutter |

## Busca

```java
editor.setSearchEnabled(true);
editor.setSearchPanelPosition(SearchPanelPosition.TOP);
editor.searchUpdateQuery("class", new SearchOptions());
editor.searchFindNext();
```

APIs relacionadas: `hideSearchPanel`, `searchFindPrev`, `searchReplaceCurrent`, `searchReplaceAll`, `isSearchPanelVisible`.

### Modo da busca

`setSearchPanelMode(SearchPanelMode)` (`dtm.stools.models.SearchPanelMode`) escolhe como o `Ctrl+F` aparece:

| Modo | Visual |
|---|---|
| `EMBEDDED` (padrão) | Barra encaixada no editor, na posição de `setSearchPanelPosition` (`TOP`, `BOTTOM`, `LEFT`, `RIGHT` ou `POPUP`) |
| `FLOATING` | Card arredondado sobre o canto superior direito do texto, como no VS Code. Ignora `setSearchPanelPosition` e empurra o inspection widget para baixo do card |

```java
editor.setSearchPanelMode(SearchPanelMode.FLOATING);
```

Trocar o modo com a busca aberta remonta o painel na hora, mantendo o termo e a linha de substituição. O mesmo enum existe no [SmartTextArea](SmartTextArea.md) (`setSearchMode`).

## Visual e interacao

| Recurso | Metodos |
|---|---|
| Somente leitura | `setReadOnly`, `isReadOnly` |
| Linha atual | `setHighlightCurrentLine`, `setCurrentLineColor` |
| Borda de foco | `setFocusBorderEnabled` |
| Minimap | `setMinimapVisibilityMode` |
| Guias de indentacao | `setShowIndentGuides` |
| Marcadores de linha | `setLineChangeMarker`, `removeLineChangeMarker`, `clearLineChangeMarkers` |
| Atalhos | `setFindKeyStroke`, `setFormatKeyStroke`, `setAutoCompleteKeyStroke`, varios outros |

## Navegação por diagnósticos

| Atalho | Ação |
|---|---|
| `F2` | Próximo diagnóstico |
| `Shift+F2` | Diagnóstico anterior |

Cada atalho seleciona o trecho do diagnóstico, centraliza a linha e abre o popup de hover com a severidade, a origem e a mensagem. A lista é percorrida em ordem de posição e dá a volta no fim; diagnósticos só de esmaecimento (`unnecessary` + `HINT`) são pulados. Cada salto entra no histórico de navegação (`Ctrl+Alt+←`).

| Método | Uso |
|---|---|
| `jumpToNextDiagnostic()` / `jumpToPreviousDiagnostic()` | Mesma ação via código |
| `setNextDiagnosticKeyStroke` / `setPreviousDiagnosticKeyStroke` | Troca os atalhos; `null` desativa |

O `F8` continua com os bookmarks (`F8` alterna, `Shift+F8` próximo, `Ctrl+Shift+F8` anterior).

## Breakpoints, bookmarks e folding

```java
editor.enableBreakpoint(true);
editor.setBreakpointEnableOnClick(true);
editor.addBreakpoint(10);

editor.addBookmark(4);
editor.jumpToNextBookmark();

editor.jumpToNextDiagnostic();

editor.setFoldingEnabled(true);
```

O gutter e organizado por layers, como line number, breakpoint, bookmark, folding e line marker.

## Providers

O editor e estendido por contratos. Exemplos:

| Provider | Responsabilidade |
|---|---|
| `TokenizerCodeEditorProvider` | Quebrar texto em tokens |
| `TokenClassifierCodeEditorProvider` | Classificar tokens |
| `TokenColorProvider` | Definir cores |
| `DiagnosticsProvider` | Erros, warnings e infos |
| `AutoCompleteProvider` | Sugestoes |
| `CodeFormatter` | Formatacao |
| `HoverDocumentationProvider` | Documentacao ao passar mouse |
| `DefinitionProvider` | Go to definition |
| `ReferencesProvider` | Find references |
| `RenameProvider` | Rename symbol |
| `CodeActionProvider` | Quick fixes |
| `DocumentSymbolProvider` | Estrutura do documento |
| `ContextMenuProvider` | Menu de contexto |

`HoverDocumentationProvider` aceita texto puro, HTML e Markdown via `new HoverInfo(texto)`, `HoverInfo.html(html)` e `HoverInfo.markdown(markdown)`.

Veja [CodeEditor_Contratos.md](CodeEditor_Contratos.md) para os contratos detalhados.

## Exemplo de menu de contexto

```java
editor.addProvider((ContextMenuProvider) e -> {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem item = new JMenuItem("Mostrar selecao");
    item.addActionListener(ev -> {
        String selected = editor.getTextArea().getSelectedTextOrEmpty();
        JOptionPane.showMessageDialog(editor, selected);
    });
    menu.add(item);
    return menu;
});
```

## Edicoes programaticas

```java
editor.beginCompoundEdit();
try {
    editor.applyEdits(List.of(new TextEdit(range, "novo texto")));
} finally {
    editor.endCompoundEdit();
}
```

Use compound edit quando varias alteracoes devem virar uma unica acao de undo.

## Cuidados

- Providers devem ser rapidos; tarefas pesadas precisam ir para background e voltar para a EDT ao atualizar UI.
- Ao manipular offsets, linhas e colunas, confira se a API espera base 0 ou base 1 no contrato usado.
- Bloqueie o editor durante operacoes longas que nao devem receber input do usuario.
- Para linguagem real, implemente tokenizer, classifier, colors, diagnostics e autocomplete como providers separados.


### Independent indentation width

`CodeEditor.setIndentSize(int)` configures indentation columns separately from `setTabSize(int)`,
which remains the visual width of a tab character. With tabs enabled, indentation combines tabs and
spaces to reach the requested column count. Tab, Shift+Tab, Enter, Backspace and indent guides use
this independent width. `setIndentSize(0)` restores the previous behavior (follow tab size and infer guides).
`getIndentSize()` returns the effective width. `FormatContext.indentSize()` exposes it to formatters;
the existing six-argument constructor remains available and defaults indent size to tab size.
