# WordEditor

`WordEditor` reúne edição rica de documentos, ribbon, navegação, paginação Java2D, histórico e arquivos em um componente Swing. Ele usa modelo e codec DOCX próprios. O editor continua em evolução: esta página descreve as funcionalidades implementadas na versão atual e delimita o subconjunto DOCX suportado.

## Pacotes

`dtm.stools.component.panels.editor` reúne `code`, `word` e `sheet`. Todo contrato, comando, modelo, configuração, provider e codec do Word fica dentro de `dtm.stools.component.panels.editor.word`.

O componente principal é `WordEditor extends BlockingPanel implements AutoCloseable`. Ele pode ser usado ao lado de [CodeEditor](CodeEditor.md) e [SheetEditor](SheetEditor.md) na mesma aplicação.

## Utilização

```java
import dtm.stools.component.panels.editor.word.WordEditor;
import dtm.stools.component.panels.editor.word.api.WordViewMode;

WordEditor editor = new WordEditor();
editor.setText("Título\nTexto do documento");
editor.setNavigationVisible(true);
editor.setRibbonVisible(true);
editor.setZoom(1.0);
editor.setViewMode(WordViewMode.PRINT_LAYOUT);
editor.setErrorHandler(error -> System.err.println(error.getMessage()));
frame.add(editor);
```

Criar, configurar e editar na EDT. `setText` e `setDocument` substituem explicitamente o documento aberto e limpam seu histórico. `insertText`, comandos, formatação e preenchimento de modelos são operações editáveis com histórico. `setReadOnly` também bloqueia comandos de mutação na sessão.

Chamar `editor.close()` quando o componente for descartado permanentemente. Remover e reinserir o painel em outra região da interface não encerra a sessão automaticamente.

## Interface, navegação e diálogos

O ribbon padrão usa grupos com ícones vetoriais, botões de estado e uma galeria de estilos. Ao reduzir a largura, os grupos passam para uma apresentação compacta e depois para um botão que abre o grupo completo. Em larguras muito pequenas, os grupos restantes ficam em **Mais**. Fonte e Parágrafo têm prioridade para permanecer visíveis. Nenhum comando depende de uma barra de rolagem horizontal no ribbon.

O painel **Navegação** tem botão de fechar e pode ser reaberto por **Exibir → Navegação**, preservando a seleção e a posição de rolagem. Seu padrão continua sendo oculto; `setNavigationVisible(true)` o exibe. Comentários também possui fechamento no próprio cabeçalho.

Os diálogos internos padrão são derivados de `DialogActivity` e pertencem à janela que contém o editor. Localizar/substituir e Comandos são não modais; formulários e confirmações são modais à janela proprietária. Editores de objetos usam `WordPropertiesActivity` com seus painéis específicos, validação e ações fora da área rolável. Cancelar, Esc ou fechar não aplica valores. Em somente leitura, os campos são protegidos e a única ação é Fechar.

Menus e paletas de cores continuam sendo popups. **Mais cores…** abre um `JColorChooser` em `DialogActivity`; **Sem cor** é uma ação explícita, diferente de cancelar. Abrir, salvar, exportar e selecionar imagens usam o seletor nativo `OsFilePicker` por padrão, com os filtros, diretório inicial e nome sugerido da operação. Cancelar retorna uma seleção vazia; ao salvar, a extensão é completada quando necessário. `setFileDialogProvider(...)` permite substituir esse comportamento, e `resetPopupProviders()` restaura o padrão nativo. A impressão mantém seu seletor próprio. Não há dependência de ModernDialog nos fluxos padrão do WordEditor.

Os providers especializados existentes continuam disponíveis. `WordDialogProvider` complementa esses contratos para entradas de texto, configuração de página, cabeçalho/rodapé, histórico, referências cruzadas e cores avançadas:

```java
WordDialogProvider customDialogs = new WordDialogProvider() {
    @Override public String id() { return "app.dialogs"; }

    @Override public <T> Optional<T> show(WordDialogRequest<T> request) {
        // Substitua este host por sua apresentação, se necessário.
        WordDialogActivity<T> activity = new WordDialogActivity<>(request);
        try { return activity.showResult(); }
        finally { activity.dispose(); }
    }
};

editor.setDialogProvider(customDialogs);
// Alternativa: editor.addProvider(customDialogs), com registro removível.
editor.setDialogProvider(null); // restaura DialogActivity padrão
editor.resetPopupProviders();   // restaura todos os providers de popup
```

`WordDialogRequest<T>` fornece proprietário, ID, título, mensagem, conteúdo Swing, fornecedor do resultado, validação, rótulo da confirmação e opções de somente leitura/Enter. O provider é chamado na EDT e retorna sincronamente `Optional.empty()` quando cancelado. Implementações personalizadas devem respeitar essas opções, validar antes de confirmar e liberar seus recursos; podem usar inclusive ModernDialog. O provider genérico não substitui os providers especializados de busca, paleta, arquivos, confirmação ou propriedades.

O host padrão usa o ciclo modal síncrono do Swing, com desenho na EDT, e herda de `DialogActivity` o vínculo com `WindowContext` e a liberação de recursos. Não chama `init()`, cuja abertura é assíncrona. `editor.close()` fecha todos os diálogos padrão da instância.

`WordUiTest` cobre painéis, estados, adaptação do ribbon, providers e ciclo dos diálogos. `WordVisualSmokeTest` gera `target/word-ui-{light|dark}-{largura}-{escala}.png`; `WordDialogVisualTest` gera capturas dos formulários complexos. Para verificar escalas adicionais, executar os testes em JVMs separadas com `-DargLine=-Dflatlaf.uiScale=1.5` e `-DargLine=-Dflatlaf.uiScale=2`.

## Funcionalidades atuais

- Parágrafos e trechos formatados, fonte, tamanho, negrito, itálico, sublinhado, tachado e cores no modelo.
- Alinhamento, espaçamento automático/exato/mínimo, recuos, títulos, controle de viúvas/órfãs e regras de manter parágrafos juntos.
- Seções com tamanho, orientação, margens, colunas, numeração e cabeçalhos/rodapés próprios.
- Texto Unicode, seleção por limites de grafemas, entrada IME e exposição via `AccessibleText`.
- Desfazer/refazer, comandos transacionais e snapshots imutáveis.
- Paginação Java2D, zoom independente da composição e modo contínuo.
- Navegação por títulos, localizar, substituir todos e paleta de comandos.
- Área de transferência com fragmentos ricos entre instâncias, HTML de saída e texto simples.
- Leitura/escrita DOCX do subconjunto suportado, diagnóstico e preservação de conteúdo desconhecido.
- Salvamento atômico, detecção de alteração externa e proteção contra resultados assíncronos obsoletos.
- Exportação HTML/texto, renderização de páginas e `Printable` sobre um snapshot.
- Providers de comandos, barras, menus contextuais, IA e exportação.
- Variáveis literais `${nome}` em modelos.

Atalhos: Ctrl+B/I/U, Ctrl+C/X/V, Ctrl+Z/Y, Ctrl+A, Ctrl+F, Ctrl+S e Ctrl+Shift+P. Os atalhos da superfície podem ser alterados com `getCanvas().bind(...)` e os mapas Swing.

O exemplo executável é `dtm.stools.examples.WordEditorExample`, em `src/test/java`. Configurar essa classe como aplicação Java na IDE com o classpath de testes.

## Arquivos e tarefas assíncronas

```java
editor.open(path).completion().whenComplete((result, error) -> {
    if (error != null) mostrarErro(error.getMessage());
});

var task = editor.save(destination);
int progress = task.progress();
task.cancel();

editor.export(htmlPath, WordEditor.ExportFormat.HTML);
editor.export(textPath, WordEditor.ExportFormat.TEXT);
```

Os arquivos são processados fora da EDT. A publicação de resultados ocorre na EDT. O progresso é por etapas, de 0 a 100. Cancelar depois do início da substituição atômica do arquivo retorna `false`: um arquivo já publicado não será apresentado como uma operação cancelada.

`open(path)` rejeita a abertura se houver alterações não salvas. `open(path, true)` é a decisão explícita do aplicativo de descartá-las. Edições que ocorram enquanto o arquivo é lido impedem a substituição da sessão por um resultado antigo.

`save` grava exatamente o snapshot capturado. Edições posteriores permanecem marcadas como não salvas. A substituição exige suporte a movimento atômico no sistema de arquivos; se não houver, a tarefa falha conservando o destino anterior.

## Compatibilidade DOCX

O alvo de interoperabilidade é DOCX local, tanto do Microsoft Word quanto exportado pelo Google Docs. Não há acesso online ao Google Docs nem leitura de `.doc` binário ou documentos com macros.

| Conteúdo | Situação |
|---|---|
| Texto Unicode, formatação direta e estilos de parágrafo/caractere suportados | Leitura, edição e escrita |
| Espaçamento automático, exato e mínimo; manter com o próximo, manter linhas juntas e viúvas/órfãs | Modelo, estilos herdados, composição, formulário e DOCX |
| Seções: próxima página, contínua, página par/ímpar e próxima coluna | Leitura, composição e escrita; geometria, colunas e numeração por seção |
| Cabeçalhos/rodapés por seção, primeira página e páginas pares/ímpares | Conteúdo independente por variante e vínculo com a seção anterior |
| Listas, tabelas, imagens e objetos reconhecidos pelo codec | Suporte existente; propriedades avançadas ainda podem ser aproximadas ou preservadas sem editor |
| Namespace WordprocessingML Transitional/Strict | Reconhecimento e manutenção do namespace; sem certificação de conformidade integral |
| Conteúdo/partes não suportados e não alterados | Preservação do XML, partes e relacionamentos; apresentação pode ser parcial |
| Arquivo importado sem alterações | Salvamento dos bytes originais |
| Assinatura digital, proteção de edição ou geometria de seção inválida | Somente leitura, com motivo específico |
| PDF | Exportador PDFBox em `WordPdfExportProvider` |

Diagnósticos de conteúdo preservado não tornam automaticamente todo o documento somente leitura. `getDiagnostics()` informa aproximações e conteúdo sem editor; `WordImportResult.blockingReasons()` informa impedimentos de edição. O codec rejeita alterações de geometria em colunas com larguras individuais, pois o modelo atual compõe colunas iguais. Margem de encadernação e outras propriedades de seção ainda sem composição são preservadas com diagnóstico.

### Seleção e aparência de tabelas

Clique dentro de uma tabela para abrir a aba **Tabela**. O grupo **Seleção** permite selecionar uma célula inteira, uma linha, uma coluna ou a tabela. **Ctrl+clique** dentro de uma célula também seleciona a célula inteira; clicar e arrastar normalmente continua selecionando texto. Os mesmos comandos estão disponíveis no menu do botão direito, que usa a tabela sob o ponteiro.

Para remover a estrutura, use **Excluir tabela** na aba ou no menu do botão direito. **Delete/Backspace** sobre células selecionadas limpa seu conteúdo. Todas as alterações de conteúdo podem ser desfeitas e refeitas.

**Tabela → Estilo → Cores alternadas…** aplica duas cores alternadas à tabela inteira ou a um intervalo definido pelos campos **Linha inicial** e **Linha final**. A numeração começa em 1 e inclui o cabeçalho; linhas fora do intervalo mantêm seus preenchimentos. A alternância começa pela primeira cor no intervalo escolhido, desconsiderando cabeçalhos preservados. Há paletas azul, verde e cinza, cores personalizadas, alternância a cada uma ou mais linhas e opção de preservar as cores das linhas marcadas como cabeçalho. O formulário mostra uma prévia. Esse recurso é opcional: novas tabelas continuam usando o formato padrão. As cores são gravadas como preenchimentos das células e preservadas no DOCX; após inserir ou reorganizar linhas, reaplique o comando para atualizar a alternância.

### Parágrafos e seções

**Página Inicial → Espaçamento → Parágrafo…** abre espaçamento antes/depois, recuos, espaçamento entre linhas e regras de paginação. Os atalhos de espaçamento continuam selecionando múltiplos automáticos. Em **Layout → Configurar página**, as opções se aplicam à seção da seleção; **Início da seção** escolhe seu tipo de quebra. Uma quebra contínua mantém a página quando a geometria do papel é compatível; uma troca de tamanho/orientação inicia outra página. Colunas de texto simples são equilibradas ao terminar uma seção contínua; balanceamento com tabelas, objetos ou notas permanece uma limitação.

O espaçamento exato usa pontos e recorta conteúdo que excede a caixa da linha. O mínimo usa o maior valor entre a medida natural e o mínimo solicitado. Regras de manter linhas juntas e viúvas/órfãs cedem quando o conteúdo não cabe em uma página, evitando paginação sem progresso. Quebras explícitas de página/coluna prevalecem.

Cabeçalhos e rodapés editam a seção atual. O formulário permite desvincular cada variante da seção anterior, mantendo seu conteúdo herdado. Alterar apenas opções ou vínculo conserva a formatação e os objetos existentes. Conteúdo com objetos que o formulário de texto não consegue editar permanece protegido contra substituição por texto. Aplicar o formulário gera uma única entrada no histórico. Páginas pares/ímpares diferentes são uma configuração global do DOCX; primeira página diferente é uma configuração de cada seção.

```java
var document = editor.getDocument();
var paragraph = document.paragraphAt(0).style()
        .withLineSpacing(WordParagraphStyle.LineSpacingRule.EXACT, 18)
        .withKeepLines(true).withWidowControl(true);
var section = document.sectionSettingsAt(0);
var continuous = section.withSection(section.section()
        .withBreakType(WordSectionProperties.BreakType.CONTINUOUS));
var effectiveHeaders = document.headersAt(0);
```

`WordPageSettings.section()` contém o tipo de início, cabeçalhos próprios, vínculos, referências originais e propriedades XML preservadas. As propriedades ficam no parágrafo que termina a seção; a última seção usa `WordDocument.pageSettings()`. `sectionSettingsAt`, `withSectionSettingsAt`, `sections` e `headersAt` permitem trabalhar com esse modelo. `WordParts.headers()` continua sendo a base global para documentos criados pela API antiga e para a opção de páginas pares/ímpares. Os construtores anteriores de `WordPageSettings`, `WordParagraphStyle`, `WordStyleProperties` e `WordLayout.Line` continuam disponíveis.

`WordFidelityTest` cobre edição e reabertura, herança e desligamento explícito de regras, caixas de linhas, páginas pares/ímpares, continuidade, balanceamento de texto, cabeçalhos vinculados/independentes, imagens, preservação de XML e histórico. Os testes de UI e os testes visuais também incluem os formulários novos. As fixtures são programáticas; homologação visual com arquivos reais e versões identificadas do Word e do Google Docs ainda não foi realizada. Fontes instaladas influenciam quebra de linha e paginação; `getFontSubstitutions()` informa substituições do JDK.

As regras de espaçamento e início de seção seguem a referência oficial de [SpacingBetweenLines](https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.wordprocessing.spacingbetweenlines.line) e [SectionType](https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.wordprocessing.sectiontype).

## Extensões

```java
import dtm.stools.component.panels.editor.word.api.ProviderRegistration;
import dtm.stools.component.panels.editor.word.provider.WordCommandProvider;

WordCommandProvider provider = new WordCommandProvider() {
    @Override public String id() { return "app.signature"; }

    @Override
    public Map<String, Action> commands(WordEditor editor) {
        return Map.of("app.insertSignature", new AbstractAction("Inserir assinatura") {
            @Override public void actionPerformed(ActionEvent event) {
                if (!editor.isReadOnly()) editor.insertText("Atenciosamente,\nEquipe");
            }
        });
    }
};

ProviderRegistration registration = editor.addProvider(provider);
registration.close(); // remove contribuições e libera recursos; operação idempotente
```

IDs de providers e comandos devem ser únicos. `attach` retorna a limpeza dos recursos instalados. `WordToolbarContributor` adiciona componentes à barra e `WordContextMenuProvider` contribui para o menu contextual.

`WordServices` injeta `DocxCodec`, `WordLayoutEngine`, `WordRenderer` e `WordUiFactory`. As classes de serviço são extensíveis, e a factory permite uma subclasse de `WordCanvas`. `setRibbon` substitui a barra padrão.

O registro completo de tipos de bloco com persistência e colaboração ainda está pendente. Providers nesta entrega estendem serviços, interface, comandos e integrações; não constituem ainda toda a infraestrutura de blocos prevista no plano.

## IA opcional e exportadores

`WordAiProvider` recebe instrução, texto do escopo explicitamente escolhido, idioma e indicador de cancelamento. O aplicativo fornece sua implementação; o núcleo não conecta serviços externos.

```java
editor.requestSuggestion("app.ai", "Melhore a clareza", false)
      .completion().thenAccept(suggestion -> {
          // Mostrar original/replacement e obter a decisão do usuário.
          // Somente depois de aceita:
          // editor.applySuggestion(suggestion);
      });
```

O argumento `false` envia somente a seleção. `true` envia o texto do documento inteiro. A sugestão nunca é aplicada automaticamente; respostas ou sugestões obsoletas são rejeitadas. Aplicar uma sugestão aceita é uma única operação de histórico.

`WordExportProvider` recebe o documento e o layout em pontos, em uma thread de trabalho. Registrar a implementação e chamar `editor.export(path, provider.id())`. O provider não deve fechar o stream recebido. O arquivo só é publicado após a exportação terminar.

`WordTemplates.variables(document)` lista variáveis e `fillTemplate(Map<String,String>)` preenche o documento numa transação. Variáveis ausentes provocam erro antes de alterar o documento; os valores são texto literal.

## Validação e próximos marcos

Executar:

```powershell
mvn -q '-Dnative.build.skip=true' '-Dlicense.skipDownloadLicenses=true' '-Dlicense.skipAddThirdParty=true' '-Dtest=WordDocumentTest,WordLayoutTest,WordEditorTest,WordIntegrationTest,WordVisualSmokeTest,DocxCodecTest,OpcPackageTest' test
```

`WordVisualSmokeTest` gera `target/word-editor-preview.png` e `target/word-page-preview.png` para inspeção. Os testes também cobrem grafemas, histórico, transações, arquivos malformados, limites ZIP, XML externo, preservação de partes, providers, arquivos alterados externamente, modelos e escopo da IA.

Continuam pendentes: composição incremental para documentos extensos, estilos herdados, listas, tabelas, imagens, seções avançadas, revisão, blocos personalizados completos, gráficos, SmartArt, equações, colaboração, recuperação persistente, adaptador PDF e homologação da matriz do Microsoft 365. A implementação atual é uma base de desenvolvimento; não é a versão estável completa descrita no plano.
