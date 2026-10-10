# PowerPointEditor

`PowerPointEditor` é um editor de apresentações Swing com ribbon, canvas Java2D, providers, histórico, PPTX e modo de apresentação dentro do próprio componente. Usa APIs do JDK e código do projeto, sem acrescentar dependências ao SwingTools.

## Uso

```java
PowerPointEditor editor = new PowerPointEditor();
editor.insertText("Título");
editor.addSlide();
editor.insertShape(PptObject.Kind.RECTANGLE);
editor.startPresentation();
// Ao descartar o editor:
editor.close();
```

Construa e modifique a UI na EDT. `startPresentation()` esconde ribbon, miniaturas, propriedades e status dentro do componente. Clique, espaço, Enter ou seta à direita avançam; seta à esquerda retrocede; Esc restaura a edição. F5 inicia a apresentação. Não é criada uma janela de tela cheia.

`getPresentation()` retorna um snapshot imutável. `getSession()` dá acesso à seleção, histórico e estado de alterações. `open(Path)`, `save()` e `save(Path)` retornam `PowerPointTask`, com progresso, conclusão e cancelamento. Abertura recusa substituir alterações não salvas, exceto com `open(path, true)`.

## Compatibilidade atual

| Área | Suporte |
|---|---|
| Slides | Criar, duplicar, excluir, renomear, reordenar, mudar fundo e tamanho da apresentação; 16:9 por padrão |
| Objetos | Texto formatado, retângulo, retângulo arredondado, elipse, losango, tabelas, conectores, imagens, áudio e vídeo; mover e redimensionar com mouse, giro e camadas |
| Texto | Arial e outras fontes instaladas, tamanhos fracionários, negrito, itálico, sublinhado e cores por trecho; quebra por largura, alinhamento horizontal/vertical, margens e espaçamento de parágrafos |
| Tabelas | Importar e editar células, linhas, colunas, dimensões, cores, bordas e mesclagens; inserir, duplicar e excluir tabelas |
| Conectores | Linhas, espessura, tracejado e pontas; arrastar extremidades e vincular às formas, acompanhando seu movimento |
| Design | Três presets de cores; sem mestres completos |
| Histórico | Desfazer e refazer edições |
| Transições | Corte, esmaecimento, varredura e empurrão; reprodução visual no componente |
| Animações | Catálogo de entrada, saída, ênfase e trajetória; sequência, início, duração, atraso e repetição; representação PresentationML e prévia Java2D aproximada |
| Mídia | WAV PCM e AVI MJPEG com players do projeto; outros codecs podem ser fornecidos por `PowerPointMediaProvider` |
| PPTX gerado | Texto, formas simples, imagens, transições, árvore nativa de animação, arquivos e relações nativas de mídia; `ppt/swingtools.xml` conserva o modelo editável com precisão |
| PPTX externo | Geometria nas dimensões reais do arquivo; texto formatado, formas, tabelas, conectores, imagens e mídia; herança básica de layout, mestre, cores e fontes de tema. Edição e inserção/exclusão de objetos suportados preservam as demais partes. Arquivos com temporização externa ficam protegidos contra edição |

O codec reabre o PPTX que ele gera e preserva os bytes originais ao salvar sem alterações. Em PPTX externos, a gravação recusa mudanças estruturais ou edições que não consiga aplicar sem perda. A árvore nativa de animação e mídia foi validada por testes de estrutura e ida e volta; a reprodução no Microsoft PowerPoint ou LibreOffice ainda não foi verificada neste ambiente. Não há paridade com todos os recursos do PowerPoint.

## Providers e ciclo de vida

`addProvider(PowerPointProvider)` registra uma extensão por ID e devolve um registro removível. Existem contratos para comandos, toolbar, diálogos, arquivos, exportação e mídia. `PowerPointMediaProvider` cria um player com `play`, `pause`, `seek`, `stop` e `close`. O projeto não inclui decoders MP3/MP4. Uma aplicação pode registrar providers para esses formatos.

Por padrão, a edição de texto usa `ModernInputDialog`; cores usam `PowerPointDialogActivity`; abertura, salvamento e inserção de mídia usam `OsFilePicker`. Providers de diálogo e arquivo registrados pela aplicação substituem esses padrões. O ribbon agrupa comandos por função e mostra uma guia contextual quando um objeto é selecionado. O exemplo inicia com FlatLaf claro e oferece alternância para FlatLaf escuro no menu **Aparência**.

A barra de slides mostra miniaturas em cartões, aceita reordenação por arraste e oferece ações no botão **⋯** e no menu de contexto. O inspetor lateral separa propriedades de slide, objeto e animações em abas. A largura das barras se ajusta em componentes menores; clique com o botão direito no canvas para abrir as ações do objeto selecionado.

Duplo clique em texto, forma ou célula inicia a edição no canvas. Use os controles de fonte do ribbon para formatar a seleção, **Ctrl+Enter** para aplicar e **Esc** para cancelar. **Ctrl+Z/Ctrl+Y** operam no texto durante a edição; a confirmação entra como uma operação no histórico da apresentação. Em tabelas, **Tab/Shift+Tab** navegam pelas células; **Shift+clique** define o intervalo para mesclar. As guias contextuais **Tabela** e **Conector** oferecem os comandos específicos. Formulários auxiliares usam `ModernComponentDialog` e confirmações usam `ModernDialog` via providers.

`insertTable(rows, columns)`, `insertConnector()`, `formatSelectedText(...)` e os comandos de tabela ampliam a API. `PptText`, `PptTable`, `PptStroke` e `PptVisual` são modelos imutáveis; tamanhos e margens usam unidades lógicas. `setSelectedFontSizePoints(double)` converte pontos para essas unidades. `PowerPointUiFactory.createTextEditor(...)` permite personalizar o editor de texto. Metadados versão 2 preservam os novos recursos; arquivos antigos continuam sendo lidos, e salvar sem alterações mantém os bytes originais.

O comando **Arquivo → Compatibilidade** mostra os diagnósticos do arquivo. Alterações incompatíveis são recusadas antes de entrar no histórico. A quantidade, ordem e tamanho dos slides externos e suas animações/transições continuam protegidos; operações sobre objetos suportados são permitidas.

`setRibbon(...)` substitui o ribbon. `PowerPointServices` permite trocar codec, renderizador e fábrica de UI. `PowerPointEditorConfig` controla somente leitura, zoom e painéis. Chame `close()` ao descartar o editor para encerrar tarefas, timers, players e providers.

## Limites

Faltam seleção múltipla de objetos, edição de notas, mestres/layouts avançados, gráficos, SmartArt, geometria personalizada e objetos incorporados. Preenchimentos complexos e alguns recursos de tipografia não têm renderização integral. Partes desconhecidas são conservadas e os recursos não exibidos reconhecidos são listados nos diagnósticos. O conjunto de efeitos é uma implementação inicial e pode diferir em aparência e tempo. Fontes e métricas do ambiente também podem alterar o resultado.

A apresentação de referência `Tema2_Apresentacao_Revista.pptx` foi usada para verificar os 18 slides, incluindo 5 tabelas e 63 conectores. A validação inclui renderização local, tema claro/escuro, edição direta e cópias de salvamento consecutivo. Microsoft PowerPoint e LibreOffice não estão instalados neste ambiente; a comparação visual com esses aplicativos permanece pendente.

Exemplo executável: `dtm.stools.examples.PowerPointEditorExample`, em `src/test/java`. Os limites da implementação atual estão descritos nesta página.
