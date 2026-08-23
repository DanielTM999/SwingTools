# Feedback e status: Badge, Progress, Alert, Steps, Pagination, Avatar e Tooltip

Componentes de `dtm.stools.component.feedback` para comunicar estado ao usuario. Todos consomem [UiTokens](UiTokens.md) e seguem o padrao de pintura de [PaintUtils](PaintUtils.md).

Para notificacoes flutuantes (toasts), continue usando `dtm.stools.context.Notifications`; estes componentes vivem dentro do layout.

## BadgeLabel

Etiqueta compacta em formato de pilula.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.feedback.badge` |
| Heranca | `BadgeLabel extends PanelEventListener` |

```java
BadgeLabel status = new BadgeLabel("Ativo", BadgeLabel.Tone.SUCCESS);
status.setShowDot(true).setStyle(BadgeLabel.Style.SOFT);
```

| Enum | Valores |
|---|---|
| `Tone` | `NEUTRAL`, `PRIMARY`, `SUCCESS`, `WARNING`, `DANGER`, `INFO` |
| `Style` | `SOFT` (fundo translucido), `SOLID` (preenchido), `OUTLINE` (so contorno) |
| `Size` | `SM`, `MD` |

| Metodo | Uso |
|---|---|
| `setText(String)` / `getText()` | Texto |
| `setTone(Tone)` / `setStyle(Style)` / `setSize(Size)` | Aparencia |
| `setShowDot(boolean)` | Ponto indicador antes do texto |
| `setCustomColor(Color)` | Cor propria, ignorando o tom |

O tamanho preferido acompanha o texto, entao a etiqueta nao estica em layouts flexiveis.

## ProgressBar

Barra linear arredondada, determinada ou indeterminada.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.feedback.progress` |
| Heranca | `ProgressBar extends PanelEventListener` |

```java
ProgressBar upload = new ProgressBar();
upload.setShowLabel(true).setTone(ProgressBar.Tone.INFO);
upload.setValue(64);
```

| Metodo | Uso |
|---|---|
| `getValue()` / `setValue(double)` / `setValue(double, boolean fireEvent)` | Valor, limitado a `[0, maximum]` |
| `setMaximum(double)` / `getMaximum()` | Escala |
| `setIndeterminate(boolean)` / `isIndeterminate()` | Bloco deslizante |
| `setAnimated(boolean)` / `setAnimationDuration(int)` | Transicao do valor |
| `setTone(Tone)` | `PRIMARY`, `SUCCESS`, `WARNING`, `DANGER`, `INFO` |
| `setBarHeight(int)` | Espessura |
| `setShowLabel(boolean)` / `setLabelFormatter(DoubleFunction<String>)` / `setLabelBold(boolean)` | Rotulo percentual |
| `setColors(Color track, Color fill, Color label)` | Cores |

Eventos: `ProgressBar.PROGRESS`, `EventType.CHANGE` e `ProgressBar.FINISHED` ao atingir o maximo.

## CircularProgress

Anel de progresso com texto central.

| Metodo | Uso |
|---|---|
| `getValue()` / `setValue(double)` / `setValue(double, boolean fireEvent)` | Valor |
| `setMaximum(double)` | Escala |
| `setIndeterminate(boolean)` / `isIndeterminate()` | Arco girando |
| `setDiameter(int)` / `setRingThickness(int)` | Geometria |
| `setShowLabel(boolean)` / `setLabelFormatter(DoubleFunction<String>)` | Texto central |
| `setColors(Color track, Color fill, Color label)` | Cores |

Evento: `CircularProgress.PROGRESS` e `EventType.CHANGE`.

As duas barras param seus timers em `removeNotify`.

## AlertPanel

Faixa de aviso em linha, com icone semantico desenhado, titulo, mensagem, acoes e botao de fechar.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.feedback.alert` |
| Heranca | `AlertPanel extends PanelEventListener` |

```java
AlertPanel aviso = new AlertPanel(AlertPanel.Severity.WARNING, "Atencao", "Verifique os dados.");
aviso.addAction(new JButton("Revisar"));
aviso.addEventListener(EventType.DISMISS, e -> registrar());
```

| Metodo | Uso |
|---|---|
| `setSeverity(Severity)` / `getSeverity()` | `INFO`, `SUCCESS`, `WARNING`, `ERROR` |
| `setTitle(String)` / `setMessage(String)` | Textos; a mensagem aceita HTML simples |
| `addAction(JComponent)` | Acao no rodape |
| `setClosable(boolean)` / `setShowIcon(boolean)` | Elementos opcionais |
| `setAccentColor(Color)` / `setArc(int)` | Aparencia |
| `dismiss()` / `restore()` | Oculta e reexibe |

Eventos: `AlertPanel.DISMISSED` e `EventType.DISMISS`.

> O metodo de reexibir chama-se `restore()`, e nao `show()`, porque `show()` pertence a `java.awt.Component`.

## StepsPanel

Indicador de etapas de um fluxo, horizontal ou vertical.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.feedback.steps` |
| Heranca | `StepsPanel extends PanelEventListener` |

```java
StepsPanel etapas = new StepsPanel();
etapas.setSteps(List.of("Carrinho", "Entrega", "Pagamento", "Revisao"));
etapas.setClickable(true);
etapas.next();
```

| Metodo | Uso |
|---|---|
| `addStep(String title, String description)` | Adiciona etapa |
| `setSteps(List<String>)` | Substitui pelas titulos |
| `getCurrentStep()` / `setCurrentStep(int)` / `setCurrentStep(int, boolean fireEvent)` | Etapa corrente |
| `next()` / `previous()` | Navegacao |
| `setClickable(boolean)` | Permite voltar a etapas ja concluidas |
| `setOrientation(Orientation)` / `setCircleSize(int)` | Geometria |
| `setColors(Color active, Color done, Color pending)` | Cores |

Etapas concluidas exibem um visto; a etapa corrente fica preenchida e em negrito. Com `setClickable(true)`, so indices menores ou iguais ao corrente respondem ao clique.

Eventos: `StepsPanel.STEP_SELECTED`, `EventType.STEP` e `EventType.CHANGE`.

## PaginationPanel

Paginacao com elipse para as faixas ocultas.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.feedback.pagination` |
| Heranca | `PaginationPanel extends PanelEventListener` |

```java
PaginationPanel paginas = new PaginationPanel(14);
paginas.addEventListener(EventType.PAGE, e -> carregar((int) e.getValue()));
```

| Metodo | Uso |
|---|---|
| `setPageCount(int)` / `getPageCount()` | Total de paginas |
| `getCurrentPage()` / `setCurrentPage(int)` / `setCurrentPage(int, boolean fireEvent)` | Pagina corrente, base zero |
| `nextPage()` / `previousPage()` | Navegacao |
| `setSiblingCount(int)` | Paginas vizinhas exibidas ao redor da corrente |
| `setButtonSize(int)` / `setColors(Color active, Color text)` | Aparencia |

O indice e sempre limitado a `[0, pageCount - 1]`. Os botoes de anterior e proximo aparecem desabilitados nas pontas.

Eventos: `PaginationPanel.PAGE_CHANGED`, `EventType.PAGE` e `EventType.CHANGE`.

## AvatarLabel

Avatar circular ou arredondado com imagem, icone ou iniciais.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.feedback.avatar` |
| Heranca | `AvatarLabel extends PanelEventListener` |

```java
AvatarLabel avatar = new AvatarLabel("Daniel Melo");
avatar.setPresence(AvatarLabel.Presence.ONLINE).setRingWidth(2).setSize(48);
```

| Metodo | Uso |
|---|---|
| `getDisplayName()` / `setDisplayName(String)` | Nome usado nas iniciais e na cor gerada |
| `getInitials()` | Iniciais derivadas do nome |
| `setImage(Image)` / `setIcon(Icon)` | Conteudo alternativo as iniciais |
| `setShape(Shape2D)` | `CIRCLE` ou `ROUNDED` |
| `setPresence(Presence)` | `NONE`, `ONLINE`, `BUSY`, `AWAY`, `OFFLINE` |
| `setSize(int)` / `setRingWidth(int)` | Geometria |
| `setColors(Color background, Color text, Color ring)` | Cores |

Sem cor de fundo explicita, a cor e derivada do hash do nome, entao a mesma pessoa recebe sempre a mesma cor.

> O nome exibido usa `setDisplayName`, e nao `setName`, porque `setName` pertence a `java.awt.Component`.

## ModernTooltip

Balao de dica arredondado com seta, instalavel em qualquer `JComponent`.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.feedback.tooltip` |
| Tipo | Classe final, instanciada por `install` |

```java
ModernTooltip.install(botao, "Salvar alteracoes")
             .setPlacement(ModernTooltip.Placement.BOTTOM)
             .setShowDelay(300);
```

| Metodo | Uso |
|---|---|
| `install(JComponent, String)` | Cria e instala a dica |
| `setText(String)` | Texto do balao |
| `setPlacement(Placement)` | `TOP`, `BOTTOM`, `LEFT`, `RIGHT` |
| `setShowDelay(int)` / `setHideDelay(int)` / `setOffset(int)` | Tempo e distancia |
| `setColors(Color background, Color foreground)` | Cores |
| `uninstall()` | Para os timers e descarta o balao |

O balao usa um `JWindow` sem foco, entao nao rouba o foco do componente nem interfere na navegacao por teclado.
