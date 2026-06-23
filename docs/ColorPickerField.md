# ColorPickerField

`ColorPickerField` e um campo Swing para selecionar e digitar cores.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.colorpicker` |
| Heranca | `ColorPickerField extends JPanel` |
| Formato | `ColorFormat` |

## Criacao

```java
ColorPickerField color = new ColorPickerField(ColorFormat.HEX, Color.RED);
```

## API

| Metodo | Uso |
|---|---|
| `setColor(Color)` / `getColor()` | Valor como `Color` |
| `setColorFormat(ColorFormat)` / `getColorFormat()` | Formato textual |
| `setColorFromHex(String)` | Define por hex |
| `getColorAsHex()` | Retorna `#RRGGBB` |
| `getColorAsRGB()` | Retorna texto RGB |
| `getText()` / `setText(String)` | Texto do campo |
| `setColorPreviewVisible(boolean)` | Mostra/oculta preview |
| `setPreviewSize(int, int)` | Tamanho do preview |

## Cuidados

- `setText` deve receber texto compativel com o formato atual.
- Use `setEnabled(false)` para travar campo e botao/preview juntos.
