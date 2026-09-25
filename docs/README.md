# SwingTools — documentação de uso

Esta pasta é a referência da **SwingTools 1.3.0**, compilada com Java 25. As páginas explicam como integrar cada recurso em uma aplicação Swing. O [README principal](../README.md) mostra instalação via JitPack, requisitos e uma visão geral; o [Guia do Desenvolvedor](Guia_do_Desenvolvedor.md) ensina a montar uma aplicação.

As áreas mais completas da biblioteca são a composição de áreas de trabalho ([abas](TabbedPanel.md), [dock](DockPanel.md), [janelas internas](WindowPanel.md)), os três editores ([código](CodeEditor.md), [documentos](WordEditor.md), [planilhas](SheetEditor.md)) e os componentes de dados e formulários ([tabela](GridView.md), [árvore](TreeView.md), [formulário](FormPanel.md)). Use a tabela abaixo para partir da tarefa da aplicação.

## Encontre o que precisa fazer

| Quero... | Comece por | Aprofunde em |
|---|---|---|
| Criar uma janela ou separar comportamento da tela | [Guia do Desenvolvedor](Guia_do_Desenvolvedor.md) | [ViewPanel](ViewPanel.md), [Eventos](Eventos.md) |
| Montar um formulário com validação | [FormPanel](FormPanel.md) | [campos de entrada](#inputs) e [dialogs](#menus-dialogs-e-janela) |
| Criar uma interface com abas e áreas móveis | [TabbedPanel](TabbedPanel.md) | [DockPanel](DockPanel.md), [WindowPanel](WindowPanel.md) |
| Mostrar dados em tabela ou árvore | [GridView](GridView.md) | [TreeView](TreeView.md) |
| Oferecer edição de código, documentos ou planilhas | [CodeEditor](CodeEditor.md) | [WordEditor](WordEditor.md), [SheetEditor](SheetEditor.md) |
| Alterar cores, tipografia e desenho | [JsonLookAndFeel](JsonLookAndFeel.md) | [UiTokens](UiTokens.md), [PaintUtils](PaintUtils.md) |
| Escolher arquivo ou usar recursos nativos | [FilePickerInputPanel](FilePickerInputPanel.md) | [OsFilePicker](OsFilePicker.md), [Graphics](Graphics.md) |

Os exemplos de cada página são trechos Java para copiar e adaptar. Quando o trecho não mostra uma classe completa, coloque-o dentro da criação da tela na EDT. Os exemplos executáveis completos ficam em `src/test/java/dtm/stools/examples`.

## Caminho recomendado de leitura

1. Leia o [Guia do Desenvolvedor](Guia_do_Desenvolvedor.md) para instalar, montar uma janela e entender ciclo de vida, controllers e EDT.
2. Consulte [Eventos](Eventos.md) ao conectar callbacks ou gerenciar inscrições.
3. Escolha um componente nas tabelas abaixo e siga o exemplo de uso antes de personalizar a API.
4. Compare com as demos em `src/test/java/dtm/stools/examples` quando precisar de uma tela completa executável.

## Mapa rapido de heranca

```text
IWindow
  Activity extends JFrame
  DialogActivity extends JDialog
  FragmentActivity extends JDialog
  TransientPopupActivity extends JWindow
    NotificationActivity

IWindowComponent
  ViewPanel extends JPanel
    BlockingPanel
      CodeEditor
      WordEditor
      SheetEditor
      PanelEventListener
        KeyPanel
        TabbedPanel
        DockPanel
        WindowPanel
        WindowDesktopPanel
        SwitchField
        CheckBoxField
        RadioField<T>
        RadioGroupField<T>
        SegmentedField<T>
        SliderField
        RatingField
        PinField
        StepperField
        TextAreaField
        DualListField<T>
        FormPanel
        FormField
        CardPanel
          StatCard
        SectionPanel
        AccordionPanel
        DividerPanel
        EmptyStatePanel
        SkeletonPanel
        BreadcrumbBar
        ToolBarPanel
        BadgeLabel
        ProgressBar
        CircularProgress
        AlertPanel
        StepsPanel
        PaginationPanel
        AvatarLabel

EventListenerComponent
  PanelEventListener
  DataTableListener extends JTable
    GridView<T>
  DropdownFieldListener<T> extends JComboBox<T>
    DropdownField
  JTextFieldListener extends JTextField
    MaskedTextField
      CurrencyField
    SearchTextField<T>
      PathSearchTextField
  AbstractGraphicsPanel<C extends GraphicsContext> extends JPanel
    GraphicsGlPanel
```

## Base e infraestrutura

| Tema | Arquivo | O que documenta |
|---|---|---|
| Guia geral | [Guia_do_Desenvolvedor.md](Guia_do_Desenvolvedor.md) | Arquitetura, heranca, ciclo de vida, binding e exemplos completos |
| Eventos | [Eventos.md](Eventos.md) | Contrato `EventListenerComponent`, payload, nomes de eventos e cancelamento |
| `ViewPanel` | [ViewPanel.md](ViewPanel.md) | Base para views reutilizaveis, DOM local e client state |
| `BlockingPanel` | [BlockingPanel.md](BlockingPanel.md) | Bloqueio de interacao em UI |
| `PanelEventListener` | [PanelEventListener.md](PanelEventListener.md) | Base para componentes com eventos |
| `KeyPanel` | [KeyPanel.md](KeyPanel.md) | Navegacao por chave entre paineis |
| `DelegatedBlockingPanel` | [DelegatedBlockingPanel.md](DelegatedBlockingPanel.md) | Painel com controller delegado |
| `DelegatedKeyPanel` | [DelegatedKeyPanel.md](DelegatedKeyPanel.md) | Navegacao por chave com controller delegado |
| `UiTokens` | [UiTokens.md](UiTokens.md) | Tokens centrais de cor, espacamento, raio e tipografia |
| `PaintUtils` | [PaintUtils.md](PaintUtils.md) | Rotinas de pintura, texto e easing compartilhadas |

## Inputs

| Componente | Arquivo | Uso principal |
|---|---|---|
| `JTextFieldListener` | [JTextFieldListener.md](JTextFieldListener.md) | `JTextField` com eventos |
| `MaskedTextField` | [MaskedTextField.md](MaskedTextField.md) | Texto com mascara, placeholder e read-only |
| `CurrencyField` | [CurrencyField.md](CurrencyField.md) | Campo monetario com `BigDecimal` |
| `NumberField` | [NumberField.md](NumberField.md) | Campo numerico com locale, limites e passo |
| `SearchTextField` | [SearchTextField.md](SearchTextField.md) | Busca/autocomplete assincrono |
| `PathTextField` | [PathTextField.md](PathTextField.md) | Campo de path com comportamento visual proprio |
| `PathSearchTextField` | [PathSearchTextField.md](PathSearchTextField.md) | Busca de paths |
| `DropdownField` | [DropdownField.md](DropdownField.md) | Combo box com datasource e renderer |
| `SwitchField` | [SwitchField.md](SwitchField.md) | Toggle visual com eventos |
| `TagInputField` | [TagInputField.md](TagInputField.md) | Entrada de tags |
| `ColorPickerField` | [ColorPickerField.md](ColorPickerField.md) | Seletor de cor |
| `DatePickerInputField` | [DatePickerInputField.md](DatePickerInputField.md) | Entrada de data |
| `CheckBoxField` | [CheckBoxField.md](CheckBoxField.md) | Caixa de selecao pintada, com estado indeterminado |
| `RadioGroupField` / `RadioField` | [RadioGroupField.md](RadioGroupField.md) | Escolha unica tipada, sem `ButtonGroup` |
| `SegmentedField` | [SegmentedField.md](SegmentedField.md) | Controle segmentado com indicador deslizante |
| `SliderField` | [SliderField.md](SliderField.md) | Deslizante com passo, ticks e balao de valor |
| `RatingField` | [RatingField.md](RatingField.md) | Avaliacao por estrelas, com meia estrela |
| `PinField` | [PinField.md](PinField.md) | Codigo de verificacao com uma caixa por digito |
| `StepperField` | [StepperField.md](StepperField.md) | Numerico com botoes de menos e mais |
| `TextAreaField` | [TextAreaField.md](TextAreaField.md) | Texto longo com contador, limite e auto-grow |
| `DualListField` | [DualListField.md](DualListField.md) | Duas listas com transferencia, filtro e reordenacao |

## Componentes complexos

| Componente | Arquivo | Uso principal |
|---|---|---|
| `GridView` | [GridView.md](GridView.md) | Tabela reflexiva a partir de POJOs anotados |
| `TreeView` | [TreeView.md](TreeView.md) | Arvore com nodes de dominio, busca, check e lazy load |
| `TabbedPanel` | [TabbedPanel.md](TabbedPanel.md) | Abas com chave, pin, dirty, badge, menu, drag e split |
| `DockPanel` | [DockPanel.md](DockPanel.md) | Layout de docking por regioes |
| `WindowPanel` / `WindowDesktopPanel` | [WindowPanel.md](WindowPanel.md) | Janelas internas, modalidade, snap, layout e extensao por heranca |
| `CodeEditor` | [CodeEditor.md](CodeEditor.md) | Editor de codigo extensivel |
| `WordEditor` | [WordEditor.md](WordEditor.md) | Editor de documentos com ribbon, paginação, navegação, formatação e leitura/escrita do subconjunto DOCX suportado; veja também arquivos e providers |
| `SheetEditor` | [SheetEditor.md](SheetEditor.md) | Planilha estilo Excel 365/Google Planilhas: formulas, funcoes, tabelas, graficos, tabela dinamica, XLSX/ODS/CSV e PDF |
| Contratos do `SheetEditor` | [SheetEditor_Contratos.md](SheetEditor_Contratos.md) | Providers de funcoes, dados externos, comandos, ribbon, popups, arquivos, colaboracao e IA |
| Contratos do `CodeEditor` | [CodeEditor_Contratos.md](CodeEditor_Contratos.md) | Providers, diagnostics, autocomplete, CodeLens e modelos semanticos |
| Graficos | [Graphics.md](Graphics.md) | Visao geral do pacote `graphics`, ciclo de vida, threading, input e nativos |
| `AbstractGraphicsPanel` | [AbstractGraphicsPanel.md](AbstractGraphicsPanel.md) | Base para paineis graficos com renderer, loop, FPS, input e ciclo de vida |
| `GraphicsGlPanel` | [GraphicsGlPanel.md](GraphicsGlPanel.md) | Painel OpenGL com contexto nativo, callbacks de render e helper `GL` |
| `FilePickerInputPanel` | [FilePickerInputPanel.md](FilePickerInputPanel.md) | Seletor de arquivo em Swing |
| `OsFilePicker` | [OsFilePicker.md](OsFilePicker.md) | File picker nativo via JNI |

## Menus, dialogs e janela

| Componente | Arquivo | Uso principal |
|---|---|---|
| `MenuBar` | [MenuBar.md](MenuBar.md) | Barra de menu configuravel |
| `CollapsibleMenuBar` | [CollapsibleMenuBar.md](CollapsibleMenuBar.md) | Menu recolhivel |
| `ActionPopupMenu` | [ActionPopupMenu.md](ActionPopupMenu.md) | Popup menu fluente |
| `ModernDialog` | [ModernDialog.md](ModernDialog.md) | Dialog visual moderno |
| `ModernComponentDialog` | [ModernComponentDialog.md](ModernComponentDialog.md) | Dialog moderno com componente customizado e retorno tipado |
| `ModernInputDialog` | [ModernInputDialog.md](ModernInputDialog.md) | Dialog de entrada com validacao |
| `LoadingPanel` | [LoadingPanel.md](LoadingPanel.md) | Painel de carregamento |
| `TitleMenuBar` | [TitleMenuBar.md](TitleMenuBar.md) | Barra de titulo customizada |

## Formulario

| Componente | Arquivo | Uso principal |
|---|---|---|
| `FormPanel` / `FormField` | [FormPanel.md](FormPanel.md) | Formulario em colunas, com validacao em bloco e submit |
| `Validator` / `Validators` / `ValidationResult` | [FormPanel.md](FormPanel.md) | Regras de validacao encadeaveis |
| `FormValues` | [FormPanel.md](FormPanel.md) | Leitura e escrita de valor por tipo de controle |

## Layout e superficie

| Componente | Arquivo | Uso principal |
|---|---|---|
| `CardPanel` / `StatCard` | [CardPanel.md](CardPanel.md) | Superficie arredondada e cartao de indicador |
| `AccordionPanel` / `SectionPanel` | [AccordionPanel.md](AccordionPanel.md) | Secoes colapsaveis, com modo exclusivo |
| `DividerPanel` | [LayoutPanels.md](LayoutPanels.md) | Separador com rotulo |
| `EmptyStatePanel` | [LayoutPanels.md](LayoutPanels.md) | Estado vazio com acao |
| `SkeletonPanel` | [LayoutPanels.md](LayoutPanels.md) | Placeholder de carregamento com shimmer |
| `ScrollPanel` / `ModernScrollBarUI` | [LayoutPanels.md](LayoutPanels.md) | Rolagem com barras finas |
| `SplitPanel` | [LayoutPanels.md](LayoutPanels.md) | Divisor com alca e colapso |
| `BreadcrumbBar` | [LayoutPanels.md](LayoutPanels.md) | Trilha de navegacao com colapso |
| `ToolBarPanel` | [LayoutPanels.md](LayoutPanels.md) | Barra de acoes com overflow |

## Feedback e status

| Componente | Arquivo | Uso principal |
|---|---|---|
| `BadgeLabel` | [Feedback.md](Feedback.md) | Etiqueta de status |
| `ProgressBar` / `CircularProgress` | [Feedback.md](Feedback.md) | Progresso linear e circular |
| `AlertPanel` | [Feedback.md](Feedback.md) | Aviso em linha com severidade |
| `StepsPanel` | [Feedback.md](Feedback.md) | Indicador de etapas |
| `PaginationPanel` | [Feedback.md](Feedback.md) | Paginacao com elipse |
| `AvatarLabel` | [Feedback.md](Feedback.md) | Avatar com iniciais e presenca |
| `ModernTooltip` | [Feedback.md](Feedback.md) | Balao de dica instalavel |

## Regras praticas para uso em aplicacoes

- Crie componentes Swing sempre na EDT com `SwingUtilities.invokeLater`.
- Inicie `Activity` na EDT; a montagem de `onDrawing()` ocorre na thread que chamou `init()`.
- Em `ViewPanel`, monte filhos em `onDrawing()` e considere `applyDrawingOnce()` se o painel puder ser reinserido. `onLoad()` pode repetir quando a visibilidade mudar.
- Use `setName("id")` em componentes que precisam ser encontrados por `findById` ou injetados com `@ViewRef`.
- Prefira controllers delegados quando a tela tiver regras, chamadas assincronas ou muitos listeners.
- Use `putInClient` apenas para estado local da janela/componente; para substituir uma chave use a sobrecarga com `replace=true`.
- Use os eventos do SwingTools para eventos de dominio do componente e os listeners Swing nativos para comportamento Swing puro.
- Quando uma API tem metodo `close...`, use ele antes de `remove...`; `close...` respeita eventos e regras como `closable`.
