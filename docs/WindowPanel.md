# WindowPanel e WindowDesktopPanel

`WindowPanel` representa uma janela interna e `WindowDesktopPanel` hospeda varias janelas sobrepostas. Os dois componentes possuem implementacoes padrao prontas e pontos oficiais de extensao por heranca.

```text
ViewPanel
  BlockingPanel
    PanelEventListener
      WindowPanel
      WindowDesktopPanel
```

## Uso direto

```java
WindowDesktopPanel desktop = new WindowDesktopPanel();

WindowPanel editor = desktop.openWindow(
    new WindowConfig("editor", "Editor.java", new JScrollPane(textArea))
        .bounds(new Rectangle(40, 30, 640, 420))
        .minimumSize(new Dimension(320, 220))
        .closable(true)
        .minimizable(true)
        .maximizable(true)
        .snapEnabled(true)
);

editor.style(style -> style
    .titleBarHeight(38)
    .arc(14));
```

Os estados suportados sao `NORMAL`, `MINIMIZED` e `MAXIMIZED`. O fechamento usa `WindowCloseOperation.HIDE` por padrao; use `REMOVE` para retirar definitivamente a janela do host.

Ao maximizar, a janela ocupa exatamente o workspace: a sombra e os cantos arredondados sao removidos nesse estado. O padding maximizado e zero por padrao. Margens opcionais podem ser aplicadas globalmente ou por janela:

```java
desktop.maximizedInsets(new Insets(8, 8, 8, 8));

WindowConfig config = new WindowConfig("editor", "Editor", editor)
    .maximizedInsets(new Insets(12, 16, 12, 16));
```

A configuracao da janela tem precedencia sobre a margem global do host. Use `null` na janela para voltar a herdar o valor do desktop.

## Operacoes do host

| Metodo | Funcao |
|---|---|
| `openWindow(WindowConfig)` | Cria, registra e abre uma janela pela factory do host |
| `addWindow(WindowPanel)` | Registra uma instancia existente |
| `activateWindow(...)` | Ativa e traz a janela para frente |
| `closeWindow(key)` | Solicita fechamento e respeita cancelamento |
| `removeWindow(...)` | Remove a janela e executa descarte |
| `cascade()` | Organiza janelas em cascata |
| `tileHorizontal()` / `tileVertical()` | Divide o espaco entre as janelas |
| `captureLayout()` / `restoreLayout(...)` | Salva e restaura bounds, estado, snap, ordem e janela ativa |

A restauracao usa as chaves registradas e ignora entradas desconhecidas; ela nunca tenta recriar o conteudo da aplicacao.

## Modalidade e snap

Uma configuracao com `.modal(true)` coloca um overlay sobre as outras janelas e restringe a ativacao. Por padrao, `Esc` fecha a modal quando ela for fechavel. O snap padrao reconhece esquerda, direita e os quatro quadrantes.

As regras podem ser trocadas com `placementPolicy(...)` e `snapPolicy(...)`.

## Snap Layouts no botao maximizar

O seletor visual pode ser acionado pelo botao maximizar, pelo arraste ate o topo central ou pelos dois modos. O padrao e `TOP_CENTER`. Ha layouts para metades, dois tercos, tres colunas, metade com quadrantes e quatro quadrantes.

```java
desktop.snapLayoutTrigger(WindowSnapLayoutTrigger.TOP_CENTER); // padrao
desktop.snapLayoutTrigger(WindowSnapLayoutTrigger.MAXIMIZE_BUTTON);
desktop.snapLayoutTrigger(WindowSnapLayoutTrigger.BOTH);
desktop.snapLayoutTrigger(WindowSnapLayoutTrigger.DISABLED);
```

No modo `TOP_CENTER`, arraste uma janela interna ate o topo central do `WindowDesktopPanel`. A barra de layouts aparece sobre o desktop, acompanha a vaga apontada e aplica a selecao ao soltar. A distancia e a largura da area de ativacao podem ser ajustadas com `snapLayoutTopActivationDistance(...)` e `snapLayoutTopActivationWidth(...)`.

Ao apontar uma vaga, `WindowSnapPreviewOverlay` desenha no desktop a area real selecionada e as demais vagas do layout. Depois do clique, `WindowSnapAssistOverlay` mostra miniaturas reais das outras janelas abertas dentro das vagas restantes. Cada miniatura pode ser clicada para completar o layout; `Esc` ou um clique no fundo fecha o assistente.

O recurso vem habilitado por padrao e pode ser controlado no host:

```java
desktop.snapLayoutsEnabled(false); // desabilita para todas as janelas herdadas
desktop.snapLayoutsEnabled(true)
       .snapLayoutHoverDelay(450);

// O preview permanece disponivel, mas nao pede as outras janelas:
desktop.snapAssistEnabled(false);
```

Cada janela herda a configuracao do host, mas pode sobrescreve-la:

```java
WindowConfig config = new WindowConfig("tool", "Ferramentas", content)
        .snapLayoutsEnabled(false)
        .snapLayoutTrigger(WindowSnapLayoutTrigger.MAXIMIZE_BUTTON);

window.snapLayoutsEnabled(false);
window.inheritSnapLayoutsEnabled();
window.snapLayoutTrigger(WindowSnapLayoutTrigger.TOP_CENTER);
window.inheritSnapLayoutTrigger();
```

Tambem e possivel aplicar uma zona programaticamente:

```java
desktop.applySnapLayout(window, WindowSnap.THIRD_CENTER);
desktop.applySnapLayout(window, WindowSnap.TWO_THIRDS_RIGHT);
```

Para personalizar o design ou os modelos, herde `WindowSnapLayoutPopup` e sobrescreva `createLayouts()`, `createLayoutPreview(...)`, `createZoneButton(...)` ou os resolvedores de cor. `WindowSnapDragSelector`, `WindowSnapPreviewOverlay` e `WindowSnapAssistOverlay` tambem sao herdaveis; o assistente expoe factories protegidas para zonas, miniaturas e captura de preview. No host, sobrescreva `createSnapLayoutPopup(...)`, `createSnapDragSelector()`, `createSnapPreviewOverlay()`, `createSnapAssistOverlay()`, os respectivos hooks de configuracao, `canShowSnapLayouts(...)`, `resolveSnapAssistCandidates(...)` ou o listener de hover.

O ciclo expoe `SNAP_LAYOUTS_CHANGE`, `SNAP_LAYOUT_TRIGGER_CHANGE`, `SNAP_LAYOUT_DRAG_OPEN`, `SNAP_LAYOUT_DRAG_CLOSE`, `SNAP_LAYOUT_MENU_OPEN`, `SNAP_LAYOUT_MENU_CLOSE`, `SNAP_LAYOUT_PREVIEW_CHANGE`, `BEFORE_SNAP_LAYOUT_SELECT`, `SNAP_LAYOUT_SELECT`, `SNAP_ASSIST_CHANGE`, `SNAP_ASSIST_OPEN`, `SNAP_ASSIST_CLOSE`, `BEFORE_SNAP_ASSIST_SELECT` e `SNAP_ASSIST_SELECT`. As duas selecoes anteriores sao cancelaveis e todos os eventos sao encaminhados ao controller delegado.

## Animacoes

Animacoes sao fornecidas por `WindowAnimator`. O `DefaultWindowAnimator` usa easing `ease-out cubic`, bounds e fade para abrir, fechar, minimizar, restaurar, maximizar e aplicar snap. Ele anima automaticamente quando o componente esta visivel e usa conclusao imediata em testes/headless.

```java
desktop.animationsEnabled(true)
       .animationDuration(220);

window.onAnimationStart(event ->
    System.out.println(event.getAnimationType()));
window.onAnimationProgress(event ->
    progressBar.setValue(Math.round(event.getProgress() * 100)));
window.onAnimationEnd(event -> progressBar.setValue(100));
```

Use `window.cancelAnimation()` para interromper a transicao atual e restaurar os bounds anteriores. Para trocar completamente o motor, forneca outro `WindowAnimator` ou sobrescreva `createWindowAnimator()` no host.

## Barra minimizada fixa ou retratil

A barra inferior permanece fixa por padrao enquanto existirem janelas minimizadas. O modo retratil, semelhante ao ocultar automaticamente da barra de tarefas do Windows, precisa ser habilitado explicitamente:

```java
WindowDesktopPanel desktop = new WindowDesktopPanel(true);

// Ou depois da criacao:
desktop.minimizedBarAutoHideEnabled(true);
```

No modo retratil, a barra recolhe ate uma faixa inferior de 3 pixels, expande quando o mouse alcanca essa faixa e volta a recolher depois do atraso. O comportamento pode ser ajustado:

```java
desktop.getMinimizedBar()
       .expandedHeight(40)
       .collapsedHeight(3)
       .collapseDelay(700)
       .animationDuration(170);

desktop.expandMinimizedBar();
desktop.collapseMinimizedBar();
```

Os eventos `MINIMIZED_BAR_EXPAND`, `MINIMIZED_BAR_COLLAPSE` e `MINIMIZED_BAR_AUTO_HIDE_CHANGE` tambem estao disponiveis no host e no controller delegado.

### Menu de contexto da barra minimizada

O menu aberto com o botao direito, semelhante ao da barra de tarefas do Windows, e opcional e fica desabilitado por padrao. Quando habilitado, o menu padrao oferece restaurar, maximizar e fechar:

```java
desktop.minimizedBarContextMenuEnabled(true);

// Alias com nomenclatura de taskbar:
desktop.taskbarContextMenuEnabled(true);
```

O menu pode ser totalmente substituido para criar uma jump list, arquivos recentes ou acoes proprias:

```java
desktop.minimizedBarMenuFactory((host, window) -> {
    JPopupMenu menu = new JPopupMenu();
    menu.add(new JMenuItem("Abrir arquivo recente"));
    menu.addSeparator();
    menu.add(new JMenuItem("Fixar projeto"));
    return menu;
});
```

Tambem e possivel herdar `DefaultWindowMinimizedMenuFactory` e sobrescrever apenas `createHeaderItem(...)`, `addWindowActions(...)` ou `createActionItem(...)`. O host expoe `createMinimizedMenuFactory()`, `configureMinimizedWindowButton(...)` e `configureMinimizedWindowMenu(...)` para especializacoes por heranca.

O ciclo publica `MINIMIZED_BAR_MENU_CHANGE`, `MINIMIZED_BAR_MENU_OPEN`, `MINIMIZED_BAR_MENU_CLOSE` e `MINIMIZED_BAR_MENU_ACTION`. `BEFORE_MINIMIZED_BAR_MENU_ACTION` e cancelavel. Todos eles possuem metodos fluentes no host e encaminhamento no `AbstractWindowDesktopController`.

## Eventos

Eventos usam `WindowEvent` e constantes de `EventWindowPanel`:

```java
window.onBeforeClose(event -> {
    if (hasUnsavedChanges()) event.cancel();
});

desktop.onActiveWindowChange(event ->
    System.out.println(event.getKey()));
```

Ha eventos de abertura, fechamento, adicao/remocao, ativacao, estado, drag, resize, bounds, snap, modalidade, propriedades, ordem, barra minimizada, animacao e alteracao/restauracao de layout.

Eventos cancelaveis incluem:

- `BEFORE_WINDOW_OPEN`, `BEFORE_WINDOW_CLOSE`, `BEFORE_WINDOW_ADD` e `BEFORE_WINDOW_REMOVE`;
- `BEFORE_WINDOW_ACTIVATE`, `BEFORE_WINDOW_MOVE` e `BEFORE_WINDOW_RESIZE`;
- `BEFORE_WINDOW_STATE_CHANGE`, `BEFORE_WINDOW_SNAP` e `BEFORE_LAYOUT_RESTORE`.
- `BEFORE_MINIMIZED_BAR_MENU_ACTION`.
- `BEFORE_SNAP_LAYOUT_SELECT`.
- `BEFORE_SNAP_ASSIST_SELECT`.

O ciclo de animacao usa `WindowAnimationEvent`: `WINDOW_ANIMATION_START`, `WINDOW_ANIMATION_PROGRESS`, `WINDOW_ANIMATION_END` e `WINDOW_ANIMATION_CANCEL`. Mudancas dinamicas em titulo, icone, conteudo, bounds, capacidades e modalidade tambem possuem eventos proprios.

## Extensao por heranca

As implementacoes padrao nao sao `final`. O host expoe factories protegidas para janela, camadas, overlay, barra minimizada, menu da barra minimizada, popup de Snap Layouts, layout, posicionamento e snap. A janela expoe factories para barra de titulo, botoes, host de conteudo, handlers, estilo e etapas de pintura.

```java
class IdeWindow extends WindowPanel {
    IdeWindow(WindowConfig config) {
        super(config);
    }

    @Override
    protected WindowTitleBar createTitleBar() {
        return new IdeTitleBar(this);
    }
}

class IdeDesktop extends WindowDesktopPanel {
    @Override
    protected WindowPanel createWindow(WindowConfig config) {
        return new IdeWindow(config);
    }
}
```

Prefira sobrescrever uma factory ou hook especifico e chamar `super` nos hooks de ciclo de vida. Assim registro, modalidade, z-order e eventos continuam consistentes.

Os cursores tambem sao extensíveis por `resolveDefaultCursor()`, `resolveTitleBarCursor()` e `resolveResizeCursor(...)`. A implementacao padrao usa a seta normal na barra e nos botoes, como um desktop nativo, e cursores direcionais somente nas bordas de resize. O drag continua ativo em toda a area livre da barra. Para mostrar outro cursor durante o drag, use `style(style -> style.titleBarCursor(...))`.

## Delegates

`DelegatedWindowPanel<T>` e `DelegatedWindowDesktopPanel<T>` adicionam controllers especializados. Implemente apenas `newController()` e sobrescreva os hooks necessarios em `AbstractWindowPanelController` ou `AbstractWindowDesktopController`.

Os controllers suportam binding por `@ViewRef` e `@ClientRef`. `HIDE` preserva o controller; fechamento com `REMOVE`, remocao direta ou `disposeController()` executam o descarte.
