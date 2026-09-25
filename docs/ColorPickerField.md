# ColorPickerField

`ColorPickerField` e um campo Swing para selecionar e digitar cores.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.colorpicker` |
| Heranca | `ColorPickerField extends JPanel` |
| Formato | `ColorFormat` |

## Criacao

```java
import dtm.stools.component.inputfields.colorpicker.ColorFormat;
import dtm.stools.component.inputfields.colorpicker.ColorPickerField;

import java.awt.Color;

ColorPickerField color = new ColorPickerField(ColorFormat.HEX, Color.RED);
Color selected = color.getColor();
String hex = color.getColorAsHex();
```

Adicione o campo a um contêiner Swing na EDT. Use `getColor()` quando a lógica da aplicação precisa de `java.awt.Color`; use `getColorAsHex()` quando precisa persistir ou exibir o valor hexadecimal. `setColor(Color)` altera a escolha programaticamente.

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
