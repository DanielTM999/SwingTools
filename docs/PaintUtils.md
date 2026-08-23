# PaintUtils

`PaintUtils` concentra as rotinas de pintura compartilhadas pelos componentes desenhados manualmente.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.utils` |
| Tipo | Classe utilitaria final |
| Uso principal | Evitar duplicacao dentro de `paintComponent` |

## Padrao de pintura

```java
@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
    try {
        Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
        paintSurface(g2, bounds);
        paintContent(g2, bounds);
    } finally {
        g2.dispose();
    }
}
```

`antialias` liga antialiasing de forma, de texto e `STROKE_PURE` de uma vez e devolve o proprio contexto, o que permite encadear com `g.create()`.

## Formas

| Metodo | Uso |
|---|---|
| `roundRect(float x, float y, float w, float h, float arc)` | Retangulo arredondado em ponto flutuante |
| `roundRect(Rectangle, float arc)` | Mesma forma a partir de um retangulo inteiro |
| `fillRoundRect(Graphics2D, Rectangle, float arc, Color)` | Preenche; ignora cor nula |
| `drawRoundRect(Graphics2D, Rectangle, float arc, Color, float stroke)` | Contorna respeitando a espessura, sem vazar meio pixel |
| `focusRing(Graphics2D, Rectangle, float arc, Color, float stroke, int gap)` | Anel de foco ao redor dos limites |
| `softShadow(Graphics2D, Shape, Color, int spread, int offsetY)` | Sombra suave por tras da forma |

## Texto

| Metodo | Uso |
|---|---|
| `fitText(FontMetrics, String, int maxWidth)` | Reduz com reticencias ate caber |
| `centeredBaseline(FontMetrics, int y, int height)` | Linha de base centralizada verticalmente |
| `drawCenteredText(Graphics2D, String, Rectangle, Color)` | Texto centralizado nos dois eixos |
| `drawLeftText(Graphics2D, String, Rectangle, Color)` | Texto a esquerda, centralizado na vertical |
| `drawPlaceholder(Graphics2D, String, Rectangle, Color, Font)` | Placeholder em italico |

Os metodos de desenho de texto ja aplicam `fitText`, entao nao ha vazamento fora dos limites informados.

## Animacao

| Metodo | Uso |
|---|---|
| `blend(Color from, Color to, float amount)` | Interpolacao linear entre cores |
| `easeInOut(float progress)` | Aceleracao e desaceleracao suaves |
| `easeOut(float progress)` | Desaceleracao suave |

O progresso deve vir de interpolacao por `System.nanoTime()`, nunca de contagem de quadros:

```java
long elapsed = System.nanoTime() - animationStartedAtNanos;
float ratio = Math.min(1f, (float) elapsed / animationRunDurationNanos);
progress = start + (target - start) * PaintUtils.easeOut(ratio);
```

Ver tambem [UiTokens.md](UiTokens.md).
