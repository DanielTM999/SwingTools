# UiTokens

`UiTokens` e a fonte central de tokens visuais (cor, espacamento, raio e tipografia) usada pelos componentes modernos do SwingTools.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.configs` |
| Tipo | Classe utilitaria final |
| Uso principal | Padronizar cor, espacamento, raio e fonte entre componentes |

## Resolucao de cor

Cada token e resolvido nesta ordem e memorizado em cache:

1. `UIManager.getColor("SwingTools.color.<token>")` — namespace publicado por [JsonLookAndFeel](JsonLookAndFeel.md)
2. a chave equivalente do Look and Feel corrente (ex.: `Panel.background`)
3. um fallback embutido, escolhido conforme o tema seja claro ou escuro

`JsonLookAndFeel.apply(...)` e `JsonLookAndFeel.updateOpenWindows()` chamam `UiTokens.refresh()` automaticamente, entao trocar de tema em runtime ja atualiza os tokens.

## Cores

| Metodo | Uso |
|---|---|
| `background()` | Fundo da janela ou area principal |
| `surface()` | Superficie de cartoes, popups e campos |
| `surfaceAlt()` | Variacao sutil da superficie |
| `foreground()` | Texto principal |
| `muted()` | Texto secundario e legendas |
| `border()` | Bordas e divisores |
| `primary()` | Acao primaria |
| `accent()` | Selecao e foco |
| `success()` / `warning()` / `danger()` / `info()` | Cores semanticas |
| `onColor(Color)` | Texto legivel sobre a cor informada |
| `hover(Color)` / `pressed(Color)` / `disabled(Color)` | Variacoes de estado |
| `overlay(Color, float alpha)` | Versao translucida |
| `isDarkTheme()` | Indica se o tema corrente e escuro |

As variacoes de estado usam [ColorUtils](../src/main/java/dtm/stools/utils/ColorUtils.java); nao escreva helpers de blend novos.

## Espacamento, raio e traco

```java
int padding = UiTokens.space(3);                    // 3 * 4px = 12px
int radius  = UiTokens.radius(UiTokens.Radius.MD);  // 10px
float line  = UiTokens.stroke();                    // 1.5f
```

| Escala de `Radius` | Pixels |
|---|---|
| `NONE` | 0 |
| `SM` | 6 |
| `MD` | 10 |
| `LG` | 14 |
| `XL` | 20 |
| `PILL` | 999 |

## Tipografia

| Metodo | Uso |
|---|---|
| `font()` | Fonte padrao da interface |
| `fontBold()` | Fonte padrao em negrito |
| `fontSmall()` | Legendas e textos auxiliares |
| `fontTitle()` | Titulos |
| `fontMono()` | Texto monoespacado |

## Escala

`scale(int)` converte pixels aplicando o fator corrente; `setScaleFactor(float)` define esse fator. Todos os tokens dimensionais passam por `scale`, entao ajustar o fator escala o catalogo inteiro.

```java
UiTokens.setScaleFactor(1.25f);
UiTokens.refresh();
JsonLookAndFeel.updateOpenWindows();
```

## Uso em um componente novo

```java
Color fill = backgroundColor != null ? backgroundColor : UiTokens.surface();
PaintUtils.fillRoundRect(g2, bounds, UiTokens.radius(UiTokens.Radius.MD), fill);
PaintUtils.drawRoundRect(g2, bounds, UiTokens.radius(UiTokens.Radius.MD), UiTokens.border(), UiTokens.stroke());
```

Ver tambem [PaintUtils.md](PaintUtils.md) e [JsonLookAndFeel.md](JsonLookAndFeel.md).
