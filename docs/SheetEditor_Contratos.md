# SheetEditor — Contratos de extensão

Todos os contratos ficam em `dtm.stools.component.panels.editor.sheet.provider`. Cada interface fica em seu próprio arquivo e estende `SheetProvider`:

```java
public interface SheetProvider {
    String id();
    default int priority() { return 0; }
    default ProviderRegistration attach(SheetEditor editor) { return ProviderRegistration.none(); }
}
```

`editor.addProvider(p)` devolve um `ProviderRegistration` idempotente. Fechá-lo remove o provider e desfaz o que ele instalou: funções, comandos, grupos do ribbon, barras de ferramentas e o que `attach` devolveu. IDs duplicados são recusados. Quando existem vários providers do mesmo tipo, vence o de maior `priority()`.

## Cálculo e dados

| Contrato | Métodos | Uso |
|---|---|---|
| `SheetFunctionProvider` | `functions()`, `localizedNames()` | Registra funções (`SheetFunction`, normalmente via `FunctionDefinition.scalar/raw(...).build()`). O motor é reconstruído ao registrar e ao remover |
| `SheetExternalDataProvider` | `supports(function)`, `fetch(ExternalDataRequest)` | Atende IMPORTRANGE, IMPORTDATA, IMPORTXML, IMPORTHTML, IMPORTFEED, WEBSERVICE, GOOGLEFINANCE, STOCKHISTORY, GOOGLETRANSLATE e CUBE*. Sem provider, essas funções devolvem erro. O núcleo não acessa a rede |
| `SheetNumberFormatProvider` | `formats()` → categoria → códigos | Acrescenta categorias e códigos ao diálogo Formatar Células |
| `SheetValidationRuleProvider` | `validate(editor, sheet, cell, value)` → `Optional<String>` | Regra extra aplicada ao confirmar a digitação. Uma mensagem presente bloqueia a entrada |
| `SheetConditionalRuleProvider` | `style(editor, sheet, row, column, value)` → `Optional<DifferentialStyle>` | Formatação condicional calculada por código |

```java
editor.addProvider(new SheetFunctionProvider() {
    public String id() { return "app.desconto"; }
    public List<SheetFunction> functions() {
        return List.of(FunctionDefinition.scalar("DESCONTO", FunctionCategory.CUSTOM, 2, 2,
                (ctx, a) -> CellValue.of(Coerce.number(a[0]) * (1 - Coerce.number(a[1]))))
                .describe("Aplica desconto", "valor", "percentual").build());
    }
});
```

## Interface

| Contrato | Métodos | Uso |
|---|---|---|
| `SheetCommandProvider` | `commands(editor)` → `Map<String, Action>`, `group()` | Ações nomeadas que ficam disponíveis em `getCommands()`, `execute(id)` e na paleta |
| `SheetRibbonContributor` | `tab()`, `group()`, `commandIds(editor)` | Acrescenta um grupo à guia indicada, que é criada se não existir |
| `SheetToolbarContributor` | `createToolbar(editor)` | Barra adicional abaixo do ribbon |
| `SheetContextMenuProvider` | `contribute(editor, SheetContextMenuContext, JPopupMenu)` | Itens nos menus de célula, cabeçalho de linha ou coluna, aba e objeto (`Target`) |
| `SheetCellRendererProvider` | `supports(CellPaintContext)`, `paint(g, ctx)` | Desenho personalizado de células (chips, progresso, ícones) |
| `SheetCellEditorProvider` | `editor(editor, sheet, cell, text, commit, cancel)` → `Optional<JComponent>` | Editor in-cell próprio, posicionado sobre a célula. `commit` grava o texto como digitado |
| `SheetChartProvider` | `supports(chart)`, `paint(g, chart, data, bounds)` | Tipos de gráfico adicionais |

Ações de comando podem declarar estas propriedades:

- `SheetRibbon.ICON` com o nome de um ícone de `SheetIcon`.
- `SheetRibbon.MENU` com uma `List<String>` de IDs, que vira um botão de menu. Use `"-"` para separador.
- `SheetRibbon.TOGGLE` junto com `Action.SELECTED_KEY` para botões de alternar.

## Popups

Os providers síncronos, chamados na EDT, substituem os diálogos padrão (`ui.popup.Default*Provider`). `resetPopupProviders()` volta aos padrões.

| Contrato | Entrada | Retorno |
|---|---|---|
| `SheetDialogProvider` | `SheetDialogRequest<T>` (owner, id, título, mensagem, conteúdo, `result`, `validate`, texto de confirmação, readOnly, modal) | `Optional<T>`, vazio quando cancelado |
| `SheetFileDialogProvider` | `SheetFileDialogRequest` (modo OPEN/SAVE/EXPORT/IMAGE/IMPORT, filtros, diretório, nome sugerido) | `Optional<Path>` |
| `SheetConfirmationProvider` | `SheetConfirmationRequest` (opções, padrão, aviso) | índice escolhido ou -1 |
| `SheetSearchPopupProvider` | `SheetSearchContext` (`findAll`, `select`, `replace`, `replaceAll`) | `SheetPopupHandle` |
| `SheetCommandPaletteProvider` | `SheetCommandPaletteContext` (`commands()`, `execute(id)`) | `SheetPopupHandle` |

Os IDs dos diálogos internos ficam em `controller.SheetDialogIds`, como `sheet.formatCells`, `sheet.insertFunction` e `sheet.nameManager`. Um `SheetDialogProvider` pode usá-los para personalizar diálogos específicos.

## Arquivos

| Contrato | Métodos | Uso |
|---|---|---|
| `SheetImportProvider` | `extensions()`, `description()`, `read(InputStream)` → `SheetImportResult` | Abre formatos adicionais e tem prioridade sobre os codecs internos para as extensões declaradas |
| `SheetExportProvider` | `extension()`, `description()`, `export(workbook, engine, SheetExportOptions, OutputStream)` | Salva e exporta formatos adicionais. `SheetPdfExportProvider` é a implementação de PDF |
| `SheetRecoveryStore` | `save`, `latest`, `history`, `clear` | Recuperação automática. `SheetFileRecoveryStore(Path)` guarda as versões em disco |

Import e export rodam fora da EDT e não fecham o stream recebido.

## Colaboração e IA

| Contrato | Métodos | Uso |
|---|---|---|
| `SheetCollaborationProvider` | `localChange(editor, SheetCollaborationEvent)`, `selectionChanged(editor, sheet, range)` | Recebe cada alteração local (id, rótulo, autor, intervalos tocados por planilha, estrutural). Alterações remotas entram por `editor.applyRemote(label, command)`, que não gera eco |
| `SheetAiProvider` | `ask(SheetAiRequest)` → `CompletionStage<SheetAiResponse>` | Usado pelo comando Assistente (Revisão) |

`SheetAiRequest` leva o tipo do pedido (sugerir fórmula, explicar, analisar dados ou livre), a instrução, a planilha, o intervalo, os dados formatados, a fórmula ativa, a localidade e um `BooleanSupplier` de cancelamento. A resposta nunca é aplicada sozinha: o botão "Aplicar Fórmula" grava `formula` em `targetCell`, ou na célula ativa, como uma operação comum do histórico.

## Comandos e sessão sem UI

- `SheetSession.execute(label, SheetCommand)` agrupa as alterações feitas por uma `SheetTransaction` (`setCell`, `updateCell`, `setStyle`, `updateProperties`, `updateAxis`, `updateWorkbook`, `replaceSheets`) em uma única entrada de desfazer.
- `SheetOperations` reúne as operações estruturais: inserir e excluir, copiar, mover, classificar, mesclar e operações de planilhas.
- `editor.edit(label, command)` faz o mesmo que `SheetSession.execute` e ainda verifica somente leitura e trata erros.
