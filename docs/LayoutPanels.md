# Paineis de layout: Divider, EmptyState, Skeleton, Scroll, Split, Breadcrumb e ToolBar

Estes componentes complementam [CardPanel](CardPanel.md) e [AccordionPanel](AccordionPanel.md) na montagem de telas. Todos consomem [UiTokens](UiTokens.md) e, quando pintam, seguem o padrao de [PaintUtils](PaintUtils.md).

## DividerPanel

Separador horizontal ou vertical, com rotulo centralizado opcional.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.divider` |
| Heranca | `DividerPanel extends PanelEventListener` |

```java
DividerPanel ou = new DividerPanel("ou");
ou.setLabelAlignment(DividerPanel.LabelAlignment.CENTER).setInset(8);
```

| Metodo | Uso |
|---|---|
| `setText(String)` | Rotulo sobre a linha; vazio desenha so a linha |
| `setOrientation(Orientation)` | `HORIZONTAL` ou `VERTICAL` |
| `setLabelAlignment(LabelAlignment)` | `START`, `CENTER` ou `END` |
| `setThickness(int)` / `setInset(int)` | Geometria |
| `setColors(Color line, Color text)` | Cores |

Com rotulo, a linha e quebrada nos dois lados do texto.

## EmptyStatePanel

Estado vazio com icone, titulo, descricao e acao.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.emptystate` |
| Heranca | `EmptyStatePanel extends PanelEventListener` |

```java
EmptyStatePanel vazio = new EmptyStatePanel("Nenhum registro", "Cadastre o primeiro item");
vazio.setDashedBorder(true).setActionButton("Novo registro");
vazio.addEventListener(EventType.ACTION, e -> abrirCadastro());
```

| Metodo | Uso |
|---|---|
| `setIcon(Icon)` | Icone acima do titulo; use `TintedIconLoader` |
| `setTitle(String)` / `setDescription(String)` | Textos |
| `setAction(JComponent)` | Componente de acao livre |
| `setActionButton(String)` | Botao pronto que dispara `EventType.ACTION` |
| `setDashedBorder(boolean)` / `setArc(int)` | Contorno |
| `setColors(Color background, Color border)` | Cores |

Eventos: `EmptyStatePanel.ACTION_TRIGGERED` e `EventType.ACTION`.

## SkeletonPanel

Placeholder de carregamento com brilho deslizante. Complementa o [LoadingPanel](LoadingPanel.md): use `SkeletonPanel` quando a forma do conteudo ja e conhecida.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.skeleton` |
| Heranca | `SkeletonPanel extends PanelEventListener` |

```java
SkeletonPanel esqueleto = new SkeletonPanel();
esqueleto.clearBlocks().addAvatarWithLines(40, 3);
```

| Metodo | Uso |
|---|---|
| `addBlock(Shape2D shape, int height, double widthRatio)` | Bloco individual (`TEXT`, `RECT` ou `CIRCLE`) |
| `addTextLines(int)` | Linhas de texto com a ultima mais curta |
| `addAvatarWithLines(int size, int lines)` | Circulo seguido de linhas |
| `clearBlocks()` / `getBlocks()` | Gerencia os blocos |
| `setAnimated(boolean)` / `setAnimationPeriod(int)` | Brilho deslizante |
| `setBlockGap(int)` | Espaco entre blocos |
| `setColors(Color base, Color highlight)` | Cores |

A animacao para em `removeNotify`, entao o painel nao consome CPU depois de removido da tela.

## ScrollPanel e ModernScrollBarUI

`JScrollPane` com barras finas, polegar arredondado e sem botoes de seta.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.scroll` |
| Heranca | `ScrollPanel extends JScrollPane`, `ModernScrollBarUI extends BasicScrollBarUI` |

```java
ScrollPanel scroll = new ScrollPanel(conteudo);
scroll.setScrollBarThickness(8).setHorizontalScrollEnabled(false);
```

| Metodo | Uso |
|---|---|
| `setScrollBarThickness(int)` | Espessura das barras |
| `setPaintTrack(boolean)` | Pinta o trilho |
| `setScrollBarColors(Color thumb, Color track)` | Cores |
| `setUnitIncrement(int)` | Passo de rolagem |
| `setHorizontalScrollEnabled(boolean)` | Liga ou desliga a barra horizontal |

`ModernScrollBarUI` pode ser aplicado isoladamente em qualquer `JScrollBar` com `scrollBar.setUI(new ModernScrollBarUI())`.

## SplitPanel

Divisor moderno com faixa fina, alca no hover e colapso por duplo clique. Complementa o [DockPanel](DockPanel.md), sem substitui-lo.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.split` |
| Heranca | `SplitPanel extends JSplitPane` |

| Metodo | Uso |
|---|---|
| `setDividerThickness(int)` | Espessura do divisor |
| `setCollapseOnDoubleClick(boolean)` | Colapso por duplo clique |
| `setDividerColors(Color divider, Color handle)` | Cores |
| `collapseFirst()` / `collapseSecond()` | Colapsa memorizando a posicao |
| `restore()` | Volta a posicao memorizada |

## BreadcrumbBar

Trilha de navegacao clicavel que colapsa os itens do meio quando falta espaco.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.breadcrumb` |
| Heranca | `BreadcrumbBar extends PanelEventListener` |

```java
BreadcrumbBar trilha = new BreadcrumbBar();
trilha.addCrumb("Inicio", "home").addCrumb("Clientes", "list").addCrumb("Daniel", "detail");
trilha.addEventListener(EventType.SELECT, e -> navegar(e.getValue()));
```

| Metodo | Uso |
|---|---|
| `addCrumb(String label, Object value)` | Adiciona ao fim |
| `setCrumbs(List<String>)` | Substitui a trilha |
| `truncateTo(int)` | Remove os itens posteriores ao indice |
| `clearCrumbs()` / `getCrumbs()` | Gerencia os itens |
| `setLastClickable(boolean)` | O item atual tambem responde a cliques |
| `setSeparatorGap(int)` | Espaco em torno do separador |
| `setColors(Color text, Color active, Color separator)` | Cores |

O ultimo item e desenhado em negrito. Quando a trilha nao cabe, os itens do meio viram reticencias, preservando o primeiro e o ultimo.

Eventos: `BreadcrumbBar.CRUMB_SELECTED` e `EventType.SELECT`, com `index` e `label` nas propriedades.

## ToolBarPanel

Barra de acoes com grupos, separadores, espacador flexivel e menu de excedente.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.toolbar` |
| Heranca | `ToolBarPanel extends PanelEventListener` |

```java
ToolBarPanel barra = new ToolBarPanel();
barra.addAction("Novo", null, this::novo)
     .addAction("Editar", null, this::editar)
     .addSeparator()
     .addSpacer()
     .addOverflowAction("Configuracoes", this::configurar);
```

| Metodo | Uso |
|---|---|
| `addItem(JComponent)` | Componente livre |
| `addAction(String text, Icon icon, Runnable action)` | Botao de acao |
| `addSeparator()` | Separador vertical |
| `addSpacer()` | Espacador que empurra os proximos itens para a direita |
| `addOverflowAction(String, Runnable)` | Acao no menu de reticencias |
| `clearItems()` | Remove tudo |
| `setItemGap(int)` / `setArc(int)` / `setPaintSurface(boolean)` | Aparencia |
| `setColors(Color background, Color border)` | Cores |

O menu de excedente reusa o [ActionPopupMenu](ActionPopupMenu.md).

Eventos: `ToolBarPanel.ACTION_TRIGGERED` e `EventType.ACTION`, com o texto da acao no valor.
