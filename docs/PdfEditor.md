# PdfEditor

`PdfEditor` é um componente Swing pronto para visualizar e editar PDFs, em `dtm.stools.component.panels.editor.pdf`. A edição funciona como em um editor de imagens: selecionar, mover, redimensionar, girar, copiar, colar, apagar e escrever direto na página, preservando texto e vetores sempre que possível. O construtor padrão instala PDFBox para leitura, renderização e edição, Bouncy Castle para assinaturas, diálogos modernos (`ModernDialog`/`ModernInputDialog`) e o seletor de arquivos nativo (`OsFilePicker`).

Tudo o que vem por padrão usa licenças permissivas, então pode ir em aplicações comerciais e fechadas: PDFBox, FontBox e Commons Logging são Apache 2.0; Bouncy Castle usa a licença Bouncy Castle (estilo MIT). Por isso o OCR **não tem provider padrão**: os motores e builds comuns de OCR trazem dependências GPL ou LGPL, como as bibliotecas que acompanham o Tesseract em algumas distribuições. Esse tipo de licença não pode ser embutido aqui sem impor as mesmas obrigações a quem usa a biblioteca. O contrato `PdfOcrProvider` é aberto: a aplicação escolhe o motor, avalia a licença e registra o provider; sem ele, o comando OCR fica desativado.

```java
PdfEditor editor = new PdfEditor();
frame.add(editor);
editor.open(Path.of("documento.pdf"));
// Ao descartar definitivamente o componente:
editor.close();
```

Crie e use o componente na EDT. `open`, `save`, `recognizePage`, `sign` e `validateSignatures` devolvem `PdfTask` e executam o trabalho pesado fora da EDT; `completion()` informa o resultado. `close()` libera documento, renderizador e executores. Mover o componente entre containers não o fecha.

## Interface

- **Ribbon** no mesmo padrão do `SheetEditor`: abas Arquivo, Página Inicial, Inserir, Organizar, Formulários, Assinar e Revisar, Exibir e a aba contextual Formato do Objeto (aparece quando uma anotação está selecionada). Os grupos usam botões grandes e pequenos e se adaptam à largura: primeiro compactam, depois viram um botão que abre o grupo e, por fim, vão para "Mais". Salvar, Desfazer e Refazer ficam no acesso rápido à direita das abas.
- **Painel lateral** redimensionável, com três seções:
  - **Páginas:** miniaturas proporcionais com destaque da página visível; clicar navega e arrastar reordena; o botão direito gira, exclui, insere ou extrai.
  - **Ferramentas:** todas as ferramentas e factories registradas.
  - **Buscar e texto:** busca no documento e texto extraído da página.
- **Seleção de páginas:** Ctrl+clique nas miniaturas adiciona ou remove páginas da seleção, e Shift+clique seleciona um intervalo. Extrair páginas (aba Arquivo ou menu da miniatura) gera um novo PDF só com as páginas escolhidas, aceitando intervalos como `1, 3-5`. A ordem digitada é respeitada (`4-2` gera as páginas invertidas). O documento aberto não é alterado.
- **Página:** modo contínuo (padrão) ou página única, com edição completa nos dois. Ctrl+roda do mouse aplica zoom mantendo o ponto sob o cursor; Ajustar à largura e Página inteira ficam no ribbon e no menu do percentual de zoom.
- **Barra de status:** página atual, ferramenta ativa, dimensões da seleção em pontos, indicador de alteração e controle de zoom.

A renderização não pisca: cada página mantém a última imagem válida até a nova ficar pronta. Uma edição invalida apenas as páginas afetadas, e inserir, mover ou excluir páginas reaproveita as imagens das demais. Miniaturas e página usam o mesmo agendador, com prioridade para a página e a requisição mais recente vencendo.

## Edição

| Ferramenta | Uso |
| --- | --- |
| Selecionar | Clique seleciona o objeto do topo; Shift+clique adiciona ou remove. Arrastar no vazio seleciona o que estiver dentro do retângulo. Arrastar o objeto move; as alças redimensionam (Shift mantém a proporção) e a alça redonda gira (Shift prende em 15°). Duplo clique edita texto, caixa de texto ou nota, e preenche campos de formulário (digita no campo de texto, marca a caixa, escolhe a opção ou o item da lista). |
| Área | Seleciona um retângulo da página e tudo que estiver nele, inclusive conteúdo que não aparece como objeto isolado. |
| Borracha | No modo Pincel (padrão), passe o pincel sobre o que quer apagar; só a parte tocada some. O tamanho fica no grupo Borracha. No modo Retângulo, arraste para apagar a área. |
| Texto | Clique para escrever a partir do ponto clicado ou arraste para definir a largura. Enter quebra linha, Ctrl+Enter ou clicar fora confirma, Esc cancela. Fonte, tamanho, cor, negrito e itálico vêm do grupo Texto. |
| Imagem, Retângulo, Elipse, Linha, Seta, Desenhar, Nota, Destacar, campos e Assinatura visual | Clique ou arraste na página. As cores de contorno e preenchimento e a espessura vêm do grupo Estilo. |

### Atalhos

| Ação | Atalho |
| --- | --- |
| Apagar a seleção | Delete ou Backspace |
| Copiar, recortar, colar, duplicar | Ctrl+C, Ctrl+X, Ctrl+V, Ctrl+D |
| Selecionar tudo na página | Ctrl+A |
| Mover a seleção | Setas (1 pt); Shift+setas (10 pt) |
| Editar o texto selecionado | F2 |
| Cancelar o gesto ou limpar a seleção | Esc |
| Desfazer e refazer | Ctrl+Z, Ctrl+Y |
| Salvar, salvar como | Ctrl+S, Ctrl+Shift+S |
| Abrir, novo, imprimir | Ctrl+O, Ctrl+N, Ctrl+P |
| Buscar | Ctrl+F |
| Zoom | Ctrl +, Ctrl −, Ctrl 0 |

O menu de contexto da página oferece recortar, copiar, colar, duplicar, editar texto, excluir, ordem e giro, além das ações de providers. As ações do item selecionado aparecem no topo. Para campos de formulário, o `PdfFormFieldMenuProvider` padrão (removível com `removeProvider("pdf.menu.formFields")`) oferece:

| Campo | Ações |
| --- | --- |
| Opções | Selecionar esta opção, adicionar opção, renomear opção, excluir opção. |
| Lista | Escolher valor, editar itens (um por linha), tamanho da fonte. |
| Campo de texto | Preencher, tamanho da fonte, várias linhas. |
| Caixa de seleção | Marcar ou desmarcar. |
| Todos | Renomear campo, obrigatório, somente leitura. |

### Como o conteúdo é alterado

O backend PDFBox analisa o fluxo de conteúdo da página e identifica cada letra, imagem, forma vetorial e grupo (XObject de formulário).

- **Apagar:** remove os operadores correspondentes. Palavras dentro de uma linha são removidas sem deslocar o restante, e o texto removido deixa de ser extraível.
- **Mover, redimensionar e girar:** imagens, formas e grupos recebem uma transformação no próprio lugar; o texto é reposicionado com a mesma fonte, cor e tamanho. Nenhuma dessas operações converte a página em imagem.
- **Borracha:** apaga só a região pincelada ou arrastada.
  - Texto: remove as letras cujo centro está na região.
  - Imagens: edita os pixels da imagem.
  - Formas vetoriais e grupos: o que está inteiro na região é removido; o que só cruza a região é recortado.
  - Anotações (traços, formas, caixas de texto, imagens inseridas): perdem só a parte apagada, e saem inteiras quando ficam totalmente cobertas.
  - Campos de formulário: são removidos, inclusive do formulário do PDF, quando o centro do campo é apagado.

  O conteúdo recortado continua no arquivo; não use a borracha como redação confidencial. Se a aparência de uma anotação for gerada de novo (por exemplo, ao reformatar uma caixa de texto), a parte apagada volta a aparecer.
- **Reconstrução da página como imagem:** só acontece quando um item não pode ser alterado diretamente (por exemplo, texto vertical ou dentro de um grupo). O editor pede confirmação antes e respeita `pageReconstructionEnabled`.

Textos novos, formas, notas, destaques, imagens inseridas e assinaturas visuais são anotações. Elas podem ser movidas, redimensionadas, giradas, recoloridas, duplicadas e copiadas entre páginas como objetos. Caixas de texto guardam fonte, tamanho e cor e podem ser reeditadas. A extração de texto do PDF não inclui o conteúdo de anotações; use `addText` para inserir texto no conteúdo da página.

### API

```java
editor.addTextBox(0, new Rectangle2D.Float(72, 700, 0, 0), "Olá", PdfTextStyle.defaults().withBold(true));
editor.addShape(0, PdfShapeKind.ELLIPSE, new Rectangle2D.Float(100, 400, 120, 60));
editor.setSelection(PdfSelection.of(0, editor.getPageElements(0)));
editor.moveSelection(20, 0);
editor.rotateSelection(90);
editor.deleteSelection();
editor.edit("Minha operação", PdfChange.forPages(0), document -> document.addText(0, "Texto", 72, 600, 12));
```

- **Coordenadas:** índices de página começam em zero e as coordenadas são pontos PDF com origem no canto inferior esquerdo.
- **Elementos e seleção:** `getPageElements(page)` lista os elementos identificáveis (`text:`, `image:`, `path:`, `form:` e `annotation:`). `PdfSelection` combina elementos e uma área opcional; `addSelectionListener` acompanha as mudanças.
- **Edições:** `edit(label, change, operation)` registra a operação no histórico. O `PdfChange` informa as páginas afetadas para que só elas sejam renderizadas de novo; sem ele, o editor invalida todas as páginas, mas continua exibindo as imagens anteriores até as novas ficarem prontas.
- **Comandos:** `getCommands()` expõe todos os comandos `pdf.*` como `Action`, e `execute(id)` executa um deles.

## Arquitetura e extensão

| Pacote | Conteúdo |
| --- | --- |
| `pdf` | `PdfEditor`, a fachada do componente. |
| `pdf.api` | Contratos e valores: `PdfDocument`, `PdfTask`, `PdfSelection`, `PdfPageElement`, `PdfTarget`, `PdfPlacement`, `PdfTextStyle`, `PdfShapeStyle`, `PdfChange`. |
| `pdf.config` | `PdfEditorConfig` (Lombok `@With`/`@Builder`) e `PdfServices`. |
| `pdf.provider` | Providers removíveis. |
| `pdf.element` | `PdfElementFactory` e as factories padrão. |
| `pdf.command` | Catálogo de comandos, histórico, área de transferência e operações de seleção. |
| `pdf.ui` | Canvas, layout de páginas, renderizador, ribbon, ícones, painel lateral, barra de status, controlador de seleção e `PdfUiFactory`. |
| `pdf.backend` | Implementação PDFBox e assinaturas. |

`addProvider(PdfProvider)` devolve um `PdfProviderRegistration`; `close()` remove a extensão e desfaz, em ordem inversa, tudo o que ela instalou. IDs duplicados são rejeitados. A maior prioridade vence e, no empate, o registro mais recente. Ao remover um provider, o anterior volta a valer.

| Provider | Função |
| --- | --- |
| `PdfBackendProvider` / `PdfDocument` | Motor de leitura, renderização e edição. |
| `PdfOcrProvider` | OCR fornecido pela aplicação. |
| `PdfSignatureProvider`, `PdfTrustProvider` | Assinatura e validação. |
| `PdfDialogProvider`, `PdfFileDialogProvider` | Diálogos e seleção de arquivos. |
| `PdfCommandProvider` | Comandos adicionais em `getCommands()`. |
| `PdfRibbonContributor` | Grupo de ribbon em uma aba existente ou nova. |
| `PdfToolbarProvider` | Barra abaixo do ribbon. |
| `PdfContextMenuProvider` | Ações no menu de contexto da página e das miniaturas. |
| `PdfElementFactory` | Ferramenta de inserção, com comando, item no ribbon e entrada na seção Ferramentas. |

### Factories de elementos

Cada ferramenta de inserção é uma `PdfElementFactory`. As padrão estão em `PdfElementFactories.defaults()` e podem ser trocadas por `PdfServices.withElementFactories(...)`. Uma factory define título, ícone, aba e grupo do ribbon, cursor e modo de posicionamento (`CLICK`, `DRAG_RECT` ou `FREEHAND`). Ao terminar o gesto, recebe um `PdfPlacement` com página, ponto, retângulo e pontos do traço.

```java
public final class CarimboAprovado implements PdfElementFactory {
    @Override public String id() { return "app.carimbo"; }
    @Override public String title() { return "Aprovado"; }
    @Override public String icon() { return "validate"; }
    @Override public PdfPlacementMode placementMode() { return PdfPlacementMode.DRAG_RECT; }
    @Override public void insert(PdfEditor editor, PdfPlacement at) throws IOException {
        Rectangle2D.Float area = at.dragged() ? at.bounds() : new Rectangle2D.Float(at.point().x, at.point().y - 40, 140, 40);
        editor.addTextBox(at.page(), area, "APROVADO", PdfTextStyle.defaults().withBold(true).withColor(new Color(0x2E8B57)));
    }
}

PdfProviderRegistration registro = editor.addProvider(new CarimboAprovado());
```

### Interface substituível

`PdfUiFactory` cria o canvas, o ribbon, o painel lateral, a barra de status e o controlador de seleção. Use `PdfServices.defaults().withUiFactory(...)`. Um ribbon diferente do `PdfRibbon` padrão continua funcionando, mas não recebe automaticamente os grupos de providers.

## OCR

`recognizePage(page, "por+eng", true)` pede o reconhecimento ao provider ativo e insere uma camada invisível pesquisável. Sem provider, o comando OCR fica desativado e a API informa que não há motor registrado. O editor não baixa nem instala binários de OCR. Implemente `PdfOcrProvider` devolvendo `PdfOcrResult` com palavras e coordenadas em pixels. Registre-o com `addProvider` ou na criação com `PdfServices.defaults().withOcr(meuOcr)`. Também é possível corrigir o resultado e chamar `addOcrLayer`.

## Assinaturas

`sign(destination, pkcs12, password, reason)` exige o documento salvo e um destino diferente da origem. `validateSignatures()` usa o `PdfTrustProvider` ativo; o padrão usa as autoridades confiáveis da JVM. O resultado distingue `VALID`, `INVALID` e `INCONCLUSIVE`. Falta de dados de revogação ou de cadeia confiável gera estado inconclusivo. O editor impede salvar sobre o arquivo assinado original; depois de editar, salve uma nova cópia.

## Arquivos, segurança e histórico

- **Extração:** `extractPages(List<Integer> pages, Path destination)` gera um PDF novo com as páginas na ordem informada. Campos de formulário cujos widgets ficaram fora são removidos; os que continuam são preservados. Campos de assinatura são retirados da cópia, porque as assinaturas deixariam de valer.
- **Salvamento:** usa arquivo temporário e substituição atômica, e recusa salvar se o arquivo foi alterado fora do editor.
- **Senha e permissões:** PDFs com senha pedem a senha pelo `PdfDialogProvider`. As permissões de modificação, extração e impressão são respeitadas; sem permissão de extração, o conteúdo da página não pode ser editado diretamente.
- **Histórico:** guarda snapshots até `historyLimit`, o que pode consumir bastante memória em documentos grandes. `isDirty()` volta a `false` quando desfazer retorna ao estado salvo.

## Limitações atuais

- Texto em fontes verticais e conteúdo dentro de XObjects de formulário não são editados letra a letra; nesses casos a página pode ser reconstruída como imagem, com confirmação.
- Novas caixas de texto usam as fontes padrão do PDF (Helvetica, Times e Courier). Caracteres fora da codificação WinAnsi aparecem como `?`. Na edição de uma palavra existente, o texto novo usa a fonte padrão mais próxima da original.
- Ao girar anotações, o retângulo da anotação passa a ser a caixa envolvente do objeto girado.
- A validação de assinaturas cobre integridade e cadeia PKIX; políticas avançadas, listas AATL/EUTL e carimbos de tempo qualificados exigem um provider especializado.

## Licenças

O build gera a pasta `licenses/` na raiz do projeto com o `license-maven-plugin`, na fase `generate-resources`. Ela contém:

- `THIRD-PARTY.txt`, com cada dependência de runtime e sua licença;
- `licenses.xml`;
- os textos das licenças, em `dependencies/`.

A mesma pasta é empacotada no JAR em `META-INF/licenses/`, inclusive no build do JitPack (`mvn install`). Dependências de teste, `provided` e `system` ficam fora da lista. O build falha se alguma dependência de runtime declarar GPL, LGPL ou AGPL.

Licenças das dependências de runtime:

| Licença | Dependências |
| --- | --- |
| Apache 2.0 | PDFBox, FontBox, Commons Logging, FlatLaf e Jackson |
| Licença Bouncy Castle (estilo MIT) | Bouncy Castle |
| MIT | JSVG e Lombok |
| BSD-2-Clause | commonmark |

O JAR não embute as dependências (só o commonmark é sombreado); consumidores Maven devem manter a resolução transitiva.
