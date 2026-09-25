# SheetEditor

`SheetEditor` é o componente de planilha do SwingTools, no estilo do Excel 365 e do Google Planilhas. `new SheetEditor()` já traz a planilha completa: faixa de opções, barra de fórmulas com caixa de nome, grade virtualizada, abas, barra de status, atalhos, diálogos e o catálogo de funções do Excel com os extras do Google. Todas as camadas podem ser trocadas por configuração, serviços ou providers, e o modelo, o motor de cálculo e os codecs funcionam sem interface.

O pacote é `dtm.stools.component.panels.editor.sheet`. Ele não importa nada de `editor.code` nem de `editor.word`.

## Pacotes

| Pacote | Conteúdo |
|---|---|
| `sheet` | `SheetEditor` (extends `BlockingPanel`, implements `AutoCloseable`) |
| `api` | `SheetSession` (transações, histórico, eventos), `SheetSelection`, `SheetTask`, `ProviderRegistration`, `CalcMode`, listeners |
| `config` | `SheetEditorConfig`, `SheetServices`, `SheetLimits` |
| `model` | `SheetWorkbook`, `SheetWorksheet`, `SheetCell`, `CellValue` e variações, `CellStyle`, `SheetStylePool`, tabelas, gráficos, validação, formatação condicional, filtros, tabelas dinâmicas, proteção e impressão |
| `store` | `CellStore` com blocos 32×32 copiados na escrita (snapshot O(1)) e `AxisIndex` para linhas e colunas |
| `formula` | Lexer, parser, AST, `FormulaLocale` (pt-BR/en-US), `Formulas`, `ReferenceAdjuster`, referências estruturadas |
| `calc` | `CalcEngine` com índice de dependências, spill, cálculo iterativo, voláteis e `PivotEngine` |
| `function` | `FunctionRegistry`, `FunctionDefinition` e as bibliotecas por categoria |
| `format` | `NumberFormatter` (códigos de formato do Excel), `ValueParser` (digitação → valor), `DateSerial` |
| `command` | `SheetCommand`, `SheetTransaction`, `SheetChange`, `History`, `SheetOperations` |
| `data` | `AutoFill`, `FlashFill`, `FilterEngine`, `ConditionalEvaluator`, `ValidationEvaluator`, `DataTools` |
| `controller` | Controladores do componente: edição, navegação, área de transferência, formatação, estrutura, dados, objetos, revisão, arquivos, comandos e popups |
| `io` | `XlsxCodec`, `OdsCodec`, `CsvCodec`, `SheetHtmlExporter`, `SheetTextExporter`, `SheetFileRecoveryStore`; `io.pdf` com `SheetPdfWriter` e `SheetPdfExportProvider` |
| `print` | `SheetPagination`, `SheetPageRenderer`, `SheetPrintable` |
| `render` | `SheetRenderer`, `ChartPainter`, `SparklinePainter` |
| `ui` | `SheetCanvas`, `SheetFormulaBar`, `SheetTabBar`, `SheetStatusBar`, `SheetRibbon`, `SheetUiFactory`; `ui.popup` com os diálogos e os providers padrão |
| `provider` | Contratos de extensão (ver [SheetEditor_Contratos.md](SheetEditor_Contratos.md)) |

## Uso básico

```java
SheetEditor editor = new SheetEditor();
editor.input("A1", "Produto");
editor.setValue("B2", 10.5);
editor.setFormula("B10", "=SOMA(B2:B9)");
frame.add(editor);
```

- `input(a1, texto)` interpreta o texto como se tivesse sido digitado: `10%`, `R$ 1.234,56`, `24/09/2026`, `'texto`, `VERDADEIRO` e fórmulas localizadas.
- `setValue` grava valores Java (`Number`, `Boolean`, `CellValue` ou texto).
- `setFormula` aceita fórmulas na localidade configurada. O texto fica guardado em en-US canônico.
- Leitura: `getValue(a1)` devolve o valor calculado, `getText(a1)` o texto formatado e `getFormula(a1)` a fórmula localizada.
- `undo()`, `redo()`, `recalculate()`, `select(ref)` e `activateSheet(i)` completam a API direta.

Configurado:

```java
SheetEditorConfig config = SheetEditorConfig.defaults()
        .withLocale(Locale.US)
        .withCalcMode(CalcMode.MANUAL)
        .withGridlinesVisible(false);
SheetEditor editor = new SheetEditor(config, SheetServices.defaults());
```

Sem interface:

```java
SheetWorkbook wb = SheetWorkbook.create();
CalcEngine engine = SheetServices.defaults().calc();
engine.attach(wb);
wb.sheet(0).put(0, 0, SheetCell.formula(Formulas.toCanonical("=SEQUENCE(10)", FormulaLocale.EN)));
engine.rebuild();
new XlsxCodec().write(wb, engine, out);
```

## Configuração

`SheetEditorConfig` é um record com `defaults()` e `withX(...)`. Os campos são:

- `readOnly` e `locale` (padrão pt-BR: separador `;`, decimal `,` e nomes de função em português).
- Visibilidade: `ribbonVisible`, `formulaBarVisible`, `headersVisible`, `gridlinesVisible`, `tabsVisible` e `statusVisible`.
- Cálculo: `zoom` (0,1 a 4), `historyLimit`, `calcMode`, `iteration` e `r1c1`.
- Edição: `autoComplete` (completa com valores da coluna), `moveAfterEnter` e `enterMovesDown`.
- `limits` (`SheetLimits.EXCEL`: 1.048.576 × 16.384 células e 512 MB por arquivo) e `autoRecoverSeconds`.

`setConfig(config)` aplica a mudança imediatamente.

`SheetServices(xlsx, csv, ods, functions, renderer, uiFactory)` permite trocar codecs, o catálogo de funções, o renderizador e a fábrica de UI (`SheetUiFactory`: canvas, barra de fórmulas, abas, status e ribbon).

## Interface

- **Grade**:
  - Pintura virtualizada, com cabeçalhos, painéis congelados, mesclagem e texto que transborda para células vazias.
  - Formatação: quebra de linha, rotação, recuo, bordas, preenchimentos, estilos de tabela e formatação condicional (barras, escalas, ícones).
  - Dados dinâmicos: borda de spill, minigráficos e caixas de seleção.
  - Indicadores: listas suspensas de validação, anotações e comentários, hiperlinks e erros.
  - Durante a edição de fórmula, os intervalos referenciados ficam coloridos.
  - Interação: alça de preenchimento, arrastar para mover ou copiar (Ctrl), redimensionar e auto-ajustar com duplo clique, zoom com Ctrl+roda e objetos (gráficos, imagens, formas, caixas de texto, segmentações) com mover e redimensionar.
- **Edição**:
  - O editor na célula e a barra de fórmulas compartilham o mesmo documento, com modos Digite, Editar (F2) e Apontar (clique ou setas inserem referências). F4 alterna `$`.
  - Autocompletar de funções e nomes, dica de argumentos e Ctrl+Shift+A para inserir os nomes dos argumentos.
  - Teclas de edição: Alt+Enter (quebra), Ctrl+Enter (preenche a seleção), Tab e Enter dentro da seleção. Parênteses faltando são fechados automaticamente.
  - Validação de dados na confirmação (Parar, Aviso e Informações), expansão automática de tabelas e detecção de URLs.
- **Ribbon**: guias Arquivo, Página Inicial, Inserir, Layout da Página, Fórmulas, Dados, Revisão e Exibir, mais as guias contextuais Design da Tabela, Design do Gráfico e Tabela Dinâmica. Os grupos se compactam conforme a largura disponível.
- **Status**: modo, referências circulares, filtro ("N de M registros"), proteção, e Média, Contagem, Soma, Mín e Máx configuráveis pelo menu de contexto. Traz também os modos de exibição e o zoom.
- **Abas**: inserir, excluir, renomear (duplo clique), duplicar, cor, ocultar e reexibir, e reordenar arrastando.
- **Paleta de comandos**: Ctrl+Shift+P lista todos os comandos registrados.

### Atalhos principais

| Grupo | Atalhos |
|---|---|
| Navegação | Setas, Shift/Ctrl/Ctrl+Shift+setas, Home, Ctrl+Home/End, PgUp/PgDn, Alt+PgUp/PgDn, Ctrl+PgUp/PgDn (abas), Ctrl+A, Ctrl+Espaço, Shift+Espaço |
| Edição | F2, Delete, Backspace, Ctrl+Z/Y, Ctrl+C/X/V, Ctrl+Alt+V, Ctrl+Shift+V (valores), Ctrl+D/R, Ctrl+E, Ctrl+; e Ctrl+Shift+; |
| Formatação | Ctrl+B/N, Ctrl+I, Ctrl+U, Ctrl+5, Ctrl+1, Ctrl+Shift+F |
| Números | Ctrl+Shift+1/2/3/4/5 e Ctrl+Shift+~ |
| Bordas | Ctrl+Shift+7 e Ctrl+Shift+- |
| Busca | Ctrl+F, Ctrl+H, Ctrl+G/F5 |
| Fórmulas | Shift+F3, Ctrl+F3, Alt+=, F9, Shift+F9, Ctrl+Alt+F9, Ctrl+\`, Ctrl+[ e Ctrl+] |
| Estrutura | Ctrl+T, Ctrl+K, Ctrl+Shift+L, Ctrl++ e Ctrl+-, Ctrl+9/0 (+Shift) |
| Arquivo e exibição | Shift+F2, Ctrl+Shift+F2, Alt+F1/F11, Alt+↓, Ctrl+S, F12, Ctrl+O, Ctrl+P, Shift+F11, Ctrl+F1 |

## Funcionalidades

- **Fórmulas e cálculo**:
  - Catálogo do Excel 365: matemática, estatística, lógica, texto, data, pesquisa, arrays dinâmicos, LET/LAMBDA/MAP/REDUCE/SCAN/BYROW/BYCOL, financeira, engenharia, informação, banco de dados, web e compatibilidade.
  - Extras do Google: QUERY, SPLIT, JOIN, FLATTEN, SORTN, REGEX*, SPARKLINE, IMPORT* via provider e outros.
  - Spill com `#SPILL!`, `A1#` e `@`, referências estruturadas, nomes definidos e cálculo iterativo.
  - Modos automático, automático exceto tabelas e manual.
- **Manipulação**:
  - Copiar, recortar e colar internamente, com ajuste de referências relativas, mesclagens, anotações e validações.
  - Colar Especial: valores, fórmulas, formatos, larguras, transpor, operações, ignorar em branco e vínculo.
  - Colar texto em TSV de/para Excel e Sheets, com HTML na cópia.
  - Preenchimento: alça, Série, Preenchimento Relâmpago, e preencher para baixo, direita, cima e esquerda.
  - Estrutura: inserir e excluir células, linhas e colunas (com ajuste de fórmulas, nomes, tabelas e objetos), ocultar, agrupar, mesclar e congelar painéis.
- **Dados**:
  - Classificação simples e personalizada em vários níveis ou por lista.
  - AutoFiltro com lista pesquisável, filtros de texto e número, 10 primeiros e Visões de Filtro.
  - Ferramentas: remover duplicatas, texto para colunas, validação (lista, número, data, comprimento, personalizada, checkbox), circular dados inválidos, atingir meta, tabela de dados, cenários, subtotal, consolidar e planilha de previsão.
- **Tabelas**: criar (Ctrl+T), 60 estilos, linha de totais com `SUBTOTAL` e referências estruturadas, opções de estilo, renomear, redimensionar, converter em intervalo, expansão automática e segmentação de dados.
- **Formatação**:
  - Formatar Células (Número, Alinhamento, Fonte, Borda, Preenchimento, Proteção), com formatos extras via provider.
  - Pincel de formatação, 27 estilos de célula, temas e ajuste automático da altura da linha ao aumentar a fonte.
  - Formatação condicional: realce, primeiros/últimos, barras, escalas, ícones, nova regra, limpar e gerenciador.
- **Objetos**: gráficos (colunas, barras, linhas, área, pizza, rosca, dispersão, bolhas, radar, combinação, histograma, cascata, funil, treemap, sunburst e outros) com editor de séries, título, legenda, rótulos e grade; imagens (arquivo ou área de transferência), formas, caixas de texto e minigráficos.
- **Tabela dinâmica**: criar em nova planilha ou em local existente, lista de campos (filtros, colunas, linhas, valores, agregação e "mostrar como"), totais gerais, atualizar e excluir. A saída é gravada nas células e pode ser consultada com `GETPIVOTDATA`.
- **Revisão**:
  - Notas e comentários encadeados (responder, resolver, navegar, listar).
  - Hiperlinks para URL, e-mail ou `#Planilha!A1`.
  - Proteção de planilha com senha e permissões, proteção da estrutura da pasta e intervalos editáveis por usuário.
  - Auditoria: nomes (gerenciador, definir, criar a partir da seleção, usar em fórmula), rastrear precedentes e dependentes, verificação de erros, avaliar fórmula e janela de inspeção.
  - Estatísticas da pasta de trabalho.
- **Impressão**: margens, orientação, tamanho, área de impressão, quebras manuais, títulos repetidos, cabeçalho e rodapé (`&P &N &D &T &A &F`), escala ou ajuste por páginas, centralização e linhas de grade. Os modos Layout da Página e Quebra de Página mostram as quebras na grade, e a impressão usa `Printable`/`Pageable`.

## Arquivos

- `open(path)` e `open(path, true)`, `save()`, `save(path)` e `export(path, ExportFormat)` devolvem `SheetTask<Path>` e fazem IO em virtual threads. O resultado é aplicado na EDT.
- **Leitura**: XLSX/XLSM, ODS, CSV/TSV/TXT (detecção de separador, codificação e localidade) e formatos de `SheetImportProvider`.
- **Gravação**: XLSX, ODS, CSV (`;` em pt-BR) e TSV. A gravação é atômica (arquivo temporário e `ATOMIC_MOVE`), detecta alteração externa e reaproveita os bytes originais quando a pasta aberta não foi alterada.
- **Exportação**: PDF (`SheetPdfExportProvider`, com páginas rasterizadas em PDF 1.4 e gerador próprio), HTML, texto e formatos de `SheetExportProvider`.
- **Importar texto** abre um assistente com delimitador, codificação, localidade e pré-visualização, e cria uma nova planilha.
- **Recuperação automática**: com um `SheetRecoveryStore` registrado (por exemplo `SheetFileRecoveryStore`), a pasta alterada é salva a cada `autoRecoverSeconds`, e o comando Recuperar lista as versões.

## Extensão

```java
ProviderRegistration r = editor.addProvider(new SheetFunctionProvider() {
    public String id() { return "app.functions"; }
    public List<SheetFunction> functions() { return List.of(minhaFuncao); }
});
r.close();
```

- **Providers**: `addProvider` recusa IDs duplicados, ordena por `priority()` e desfaz tudo (funções, comandos, grupos de ribbon, barras) quando a registration é fechada.
- **Popups**: os providers de popup substituem os diálogos padrão, e `resetPopupProviders()` volta aos padrões.
- **Comandos**: `registerCommand(id, action)` e `execute(id)`. `getCommands()` expõe todos os IDs `sheet.*`, que o ribbon, os menus, os atalhos e a paleta usam.
- **Ribbon**: `setRibbon(JComponent)` substitui a faixa inteira e `getDefaultRibbon().addGroup(tab, group)` acrescenta grupos.
- **Listeners**: `addCellChangeListener`, `addSelectionListener`, `addSessionListener` e `addCalcListener`. A propriedade `error` é disparada em falhas, e `setErrorHandler` troca o tratamento padrão, que mostra um aviso.

## Exemplo e testes

- **Exemplo**: `dtm.stools.examples.SheetEditorExample` (classpath de testes) abre uma pasta com vendas, tabela, formatação condicional, validação, minigráficos, gráfico, fórmulas dinâmicas, QUERY e uma tabela dinâmica, além de uma função personalizada `DESCONTO`.
- **Testes**: `FormulaEngineTest`, `NumberFormatTest`, `SheetIoTest`, `SheetEditorTest` e `SheetVisualSmokeTest`. Este último gera `target/sheet-ui-{light|dark}-*.png`.

```powershell
mvn -q '-Dnative.build.skip=true' '-Dlicense.skipDownloadLicenses=true' '-Dlicense.skipAddThirdParty=true' '-Dtest=FormulaEngineTest,NumberFormatTest,SheetIoTest,SheetEditorTest,SheetVisualSmokeTest' test
```

## Limitações conhecidas

- A verificação ortográfica não tem contrato próprio: o comando informa que nenhum verificador está configurado.
- A impressão ainda não desenha os títulos de linha e coluna (a opção é salva, mas ignorada pelo renderizador de páginas).
- O PDF é rasterizado, portanto o texto não é selecionável.
- Os filtros de tabela ficam em memória e não são gravados no XLSX. O AutoFiltro da planilha é gravado.
- `SheetCollaborationProvider` recebe as alterações locais e a seleção. As operações remotas entram por `applyRemote(label, command)`.
