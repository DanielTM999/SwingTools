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
- Alinhamento, espaçamento, recuos, títulos, tamanho da página e margens.
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

## Compatibilidade DOCX desta entrega

| Conteúdo | Situação |
|---|---|
| Texto, caracteres Unicode e formatação direta suportada | Leitura, edição e escrita |
| Parágrafos com propriedades suportadas e seção final simples | Leitura, edição e escrita |
| Namespace WordprocessingML Transitional/Strict | Reconhecimento e manutenção do namespace; sem certificação de conformidade integral |
| Partes opacas não alteradas | Preservadas no pacote |
| Arquivo importado sem alterações | Salvamento dos bytes originais |
| Estilos externos, configurações avançadas, listas, tabelas, imagens e outros objetos | Importação protegida; apresentação parcial com diagnósticos, sem editor completo |
| PDF | Ponto de extensão disponível; adaptador PDFBox ainda pendente |

Qualquer marcação reconhecida como não suportada torna a sessão importada somente leitura. A visualização pode conter apenas o texto disponível ou marcadores de objetos; ela não representa a aparência completa do documento original. O codec rejeita a gravação de um modelo alterado que descartaria conteúdo protegido. O aplicativo pode explicitamente criar um novo documento com o texto extraído, mas essa é uma conversão com perda.

Os testes atuais usam arquivos gerados e fixtures programáticas. A homologação com a versão congelada do Word do Microsoft 365, seus arquivos reais e a galeria completa ainda não foi realizada.

`getDiagnostics()` retorna as limitações de importação. `getFontSubstitutions()` informa as substituições de fontes feitas pelo JDK. Fontes não são distribuídas com o componente.

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
