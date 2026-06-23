# CodeEditor - contratos

## Provider base

Todo contrato de extensao implementa `CodeEditorProvider`.

## Providers de linguagem/render

| Tipo | Contrato |
|---|---|
| `TokenizerCodeEditorProvider` | Tokeniza texto |
| `TokenClassifierCodeEditorProvider` | Classifica tokens |
| `TokenColorProvider` | Resolve cores |
| `TokenRenderCodeEditorProvider` | Renderiza token |
| `BracketMatcher` | Define pares de bracket |
| `WordDetector` | Detecta palavra |
| `CommentProvider` | Regras de comentario |

## Providers de IDE

| Tipo | Contrato |
|---|---|
| `DefinitionProvider` | Resolve definicao |
| `ReferencesProvider` | Resolve referencias |
| `RenameProvider` | Produz edits de rename |
| `DocumentSymbolProvider` | Lista simbolos |
| `SelectionRangeProvider` | Ranges de selecao |
| `CodeActionProvider` | Acoes de codigo |
| `ContextMenuProvider` | Menu contextual |
| `CodeFormatter` | Formatacao |

## Recursos visuais/analise

| Tipo | Contrato |
|---|---|
| `AutoCompleteProvider` | Sugestoes |
| `DiagnosticsProvider` | Diagnosticos |
| `HoverDocumentationProvider` | Hover docs |
| `InlayHintProvider` | Hints inline |
| `CodeLensProvider` | CodeLens |

Hover docs podem ser retornados como texto puro, HTML ou Markdown usando `HoverInfo.markdown(markdown)`.

## Modelos semanticos

`Position`, `Range`, `Location`, `TextEdit`, `Command`, `CodeAction`, `DocumentSymbol`, `SymbolKind`, `CodeEditorState`.

## Gutter

| Tipo | Contrato |
|---|---|
| `GutterLayer` | Camada desenhavel |
| `LineNumberLayer` | Numeracao |
| `BreakpointLayer` | Breakpoints |
| `BookmarkLayer` | Bookmarks |
| `FoldingLayer` | Folding |
| `LineMarkerLayer` | Marcadores |

## Texto e estilo

`TextBuffer`, `Token`, `TokenType`, `FoldRule`, `FoldRegion`, `TextStyle`, `StyledRange`, `BookmarkStyle`, `BreakpointStyle`, `Breakpoint`, `LineColorInfo`.

## Listeners

`DocumentEditListener`, `LineChangeListener`, `BreakpointChangeListener`, `BookmarkChangeListener`, `CodeEditorStateListener`, `HoverListener`, `SearchRequestListener`.
