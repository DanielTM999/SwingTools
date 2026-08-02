# SwingTools - documentacao para desenvolvedores

Esta pasta e a referencia tecnica para quem vai usar ou estender o SwingTools em uma aplicacao Java Swing. O README principal do repositorio apresenta a biblioteca; estes documentos entram nos contratos de API, heranca, ciclo de vida, eventos, exemplos de uso e pontos de extensao.

## Caminho recomendado de leitura

1. Leia [Guia_do_Desenvolvedor.md](Guia_do_Desenvolvedor.md) para entender a arquitetura, a heranca e o jeito esperado de montar telas.
2. Leia [Eventos.md](Eventos.md) antes de usar componentes que emitem eventos.
3. Escolha o componente na tabela abaixo e use a doc dele como referencia de API.
4. Consulte os exemplos em `src/test/java/dtm/stools/examples` quando quiser ver uma tela completa executavel.

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
      PanelEventListener
        KeyPanel
        TabbedPanel
        DockPanel
        WindowPanel
        WindowDesktopPanel
        SwitchField

EventListenerComponent
  PanelEventListener
  DataTableListener extends JTable
    GridViewTable<T>
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

## Inputs

| Componente | Arquivo | Uso principal |
|---|---|---|
| `JTextFieldListener` | [JTextFieldListener.md](JTextFieldListener.md) | `JTextField` com eventos |
| `MaskedTextField` | [MaskedTextField.md](MaskedTextField.md) | Texto com mascara, placeholder e read-only |
| `CurrencyField` | [CurrencyField.md](CurrencyField.md) | Campo monetario com `BigDecimal` |
| `SearchTextField` | [SearchTextField.md](SearchTextField.md) | Busca/autocomplete assincrono |
| `PathTextField` | [PathTextField.md](PathTextField.md) | Campo de path com comportamento visual proprio |
| `PathSearchTextField` | [PathSearchTextField.md](PathSearchTextField.md) | Busca de paths |
| `DropdownField` | [DropdownField.md](DropdownField.md) | Combo box com datasource e renderer |
| `SwitchField` | [SwitchField.md](SwitchField.md) | Toggle visual com eventos |
| `TagInputField` | [TagInputField.md](TagInputField.md) | Entrada de tags |
| `ColorPickerField` | [ColorPickerField.md](ColorPickerField.md) | Seletor de cor |
| `DatePickerInputField` | [DatePickerInputField.md](DatePickerInputField.md) | Entrada de data |

## Componentes complexos

| Componente | Arquivo | Uso principal |
|---|---|---|
| `GridViewTable` | [GridViewTable.md](GridViewTable.md) | Tabela reflexiva a partir de POJOs anotados |
| `TreeView` | [TreeView.md](TreeView.md) | Arvore com nodes de dominio, busca, check e lazy load |
| `TabbedPanel` | [TabbedPanel.md](TabbedPanel.md) | Abas com chave, pin, dirty, badge, menu, drag e split |
| `DockPanel` | [DockPanel.md](DockPanel.md) | Layout de docking por regioes |
| `WindowPanel` / `WindowDesktopPanel` | [WindowPanel.md](WindowPanel.md) | Janelas internas, modalidade, snap, layout e extensao por heranca |
| `CodeEditor` | [CodeEditor.md](CodeEditor.md) | Editor de codigo extensivel |
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

## Regras praticas para uso em aplicacoes

- Crie componentes Swing sempre na EDT com `SwingUtilities.invokeLater`.
- Use `setName("id")` em componentes que precisam ser encontrados por `findById` ou injetados com `@ViewRef`.
- Prefira controllers delegados quando a tela tiver regras, chamadas assincronas ou muitos listeners.
- Use `putInClient` apenas para estado local da janela/componente; nao use como banco global.
- Use os eventos do SwingTools para eventos de dominio do componente e os listeners Swing nativos para comportamento Swing puro.
- Quando uma API tem metodo `close...`, use ele antes de `remove...`; `close...` respeita eventos e regras como `closable`.
