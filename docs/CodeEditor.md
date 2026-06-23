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

## Breakpoints, bookmarks e folding

```java
editor.enableBreakpoint(true);
editor.setBreakpointEnableOnClick(true);
editor.addBreakpoint(10);

editor.addBookmark(4);
editor.jumpToNextBookmark();

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
