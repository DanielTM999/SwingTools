# CardPanel e StatCard

`CardPanel` e uma superficie arredondada com cabecalho, corpo e rodape opcionais. `StatCard` estende essa superficie para exibir um indicador com valor, variacao e minigrafico.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.card` |
| Heranca | `CardPanel extends PanelEventListener`, `StatCard extends CardPanel` |
| Uso principal | Agrupar conteudo em blocos visuais |

## CardPanel

```java
CardPanel card = new CardPanel("Assinatura", "Plano e cobranca");
card.setVariant(CardPanel.Variant.ELEVATED)
    .setContent(formulario)
    .setFooter(botoes)
    .setHeaderAction(menuButton);
```

| Variante | Aparencia |
|---|---|
| `ELEVATED` | Sombra suave, sem borda permanente |
| `OUTLINED` | Borda visivel, sem sombra |
| `FILLED` | Fundo em `surfaceAlt`, com borda |

| Metodo | Uso |
|---|---|
| `setTitle(String)` / `setSubtitle(String)` | Textos do cabecalho |
| `setContent(JComponent)` | Conteudo principal |
| `setFooter(JComponent)` | Rodape; oculto quando `null` |
| `setHeaderAction(JComponent)` | Componente a direita do cabecalho |
| `setVariant(Variant)` / `getVariant()` | Aparencia da superficie |
| `setArc(int)` / `setShadowSpread(int)` / `setPadding(Insets)` | Geometria |
| `setClickable(boolean)` / `isClickable()` | Cursor de mao e `EventType.ACTION` no clique |
| `setColors(Color background, Color border)` | Cores principais |
| `getBody()` | Painel de corpo, para composicoes avancadas |

O cabecalho fica oculto automaticamente quando nao ha titulo, subtitulo nem acao. Um cartao clicavel escurece no hover e no clique, usando `UiTokens.hover` e `UiTokens.pressed`.

Evento: `EventType.ACTION`, apenas quando `setClickable(true)`.

## StatCard

```java
StatCard receita = new StatCard("Receita", "R$ 128k");
receita.setDelta("+12,4%", StatCard.Trend.UP)
       .setCaption("vs. mes anterior")
       .setSparkline(List.of(3d, 5d, 4d, 8d, 7d, 11d, 13d));
```

| Metodo | Uso |
|---|---|
| `setLabel(String)` | Rotulo do indicador |
| `setValue(String)` | Valor principal, em fonte ampliada |
| `setDelta(String, Trend)` | Variacao com seta colorida |
| `setCaption(String)` | Legenda abaixo do valor |
| `setIcon(Icon)` | Icone no canto |
| `setSparkline(List<Double>)` | Serie do minigrafico |
| `setTextColors(Color value, Color label)` | Cores do valor e do rotulo |

`Trend.UP` usa `UiTokens.success()`, `Trend.DOWN` usa `danger()` e `Trend.NEUTRAL` usa `muted()`; a mesma cor pinta a seta e o minigrafico. Uma serie com menos de dois pontos nao desenha o grafico.

Por padrao o `StatCard` nasce como `OUTLINED`; qualquer metodo do `CardPanel` continua disponivel.
