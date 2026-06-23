# JsonLookAndFeel

`JsonLookAndFeel` aplica temas visuais em aplicações Swing a partir de JSON.

Ele usa Jackson para ler o JSON e `UIManager` para registrar os valores do tema. Depois da aplicação, as janelas abertas são atualizadas com `SwingUtilities.updateComponentTreeUI` e a árvore de componentes recebe novamente os defaults de cor, fonte, margem, borda e propriedades conhecidas.

Quando uma propriedade não é informada no JSON, o valor existente no próprio Look and Feel atual é preservado.

---

## Uso rápido

```java
JsonLookAndFeel.apply(Path.of("theme.json"));
```

Também é possível aplicar a partir de `String`, `File`, `InputStream` ou `Map<String, Object>`:

```java
JsonLookAndFeel.apply(jsonString);
JsonLookAndFeel.apply(file);
JsonLookAndFeel.apply(inputStream);
JsonLookAndFeel.apply(themeMap);
```

Por padrão, `apply(...)` atualiza todas as janelas abertas. Para aplicar apenas no `UIManager`, sem atualizar janelas:

```java
JsonLookAndFeel.apply(jsonString, false);
```

Para atualizar manualmente depois:

```java
JsonLookAndFeel.updateOpenWindows();
```

Para atualizar uma árvore específica de componentes:

```java
JsonLookAndFeel.applyDefaultsToTree(meuPainel);
SwingUtilities.updateComponentTreeUI(meuPainel);
```

---

## Estrutura completa do JSON

```json
{
  "lookAndFeel": "javax.swing.plaf.nimbus.NimbusLookAndFeel",
  "font": {
    "family": "Segoe UI",
    "size": 13,
    "style": "plain"
  },
  "colors": {
    "background": "#1E1E1E",
    "foreground": "#F5F5F5",
    "text": "#F5F5F5",
    "primary": "#3B82F6",
    "accent": "#3B82F6",
    "danger": "#EF4444",
    "border": "#3A3A3A",
    "selectionBackground": "#2563EB",
    "selectionForeground": "#FFFFFF",
    "disabledForeground": "#888888"
  },
  "components": {
    "Panel": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5"
    },
    "Label": {
      "foreground": "#F5F5F5",
      "font": {
        "family": "Segoe UI",
        "size": 13
      }
    },
    "Button": {
      "background": "#3B82F6",
      "foreground": "#FFFFFF",
      "font": {
        "family": "Segoe UI",
        "size": 13,
        "style": "bold"
      },
      "padding": [8, 14],
      "borderRadius": 8
    },
    "TextField": {
      "background": "#252526",
      "foreground": "#FFFFFF",
      "selectionBackground": "#2563EB",
      "selectionForeground": "#FFFFFF",
      "caret": "#FFFFFF",
      "borderColor": "#555555",
      "padding": [6, 10]
    },
    "TextArea": {
      "background": "#252526",
      "foreground": "#FFFFFF",
      "selectionBackground": "#2563EB",
      "selectionForeground": "#FFFFFF",
      "caret": "#FFFFFF"
    },
    "ComboBox": {
      "background": "#252526",
      "foreground": "#FFFFFF",
      "selectionBackground": "#2563EB",
      "selectionForeground": "#FFFFFF"
    },
    "CheckBox": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5"
    },
    "RadioButton": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5"
    },
    "Table": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5",
      "selectionBackground": "#2563EB",
      "selectionForeground": "#FFFFFF",
      "gridColor": "#3A3A3A",
      "rowHeight": 28
    },
    "TableHeader": {
      "background": "#252526",
      "foreground": "#F5F5F5"
    },
    "List": {
      "background": "#252526",
      "foreground": "#FFFFFF",
      "selectionBackground": "#2563EB",
      "selectionForeground": "#FFFFFF"
    },
    "Tree": {
      "background": "#1E1E1E",
      "foreground": "#FFFFFF",
      "selectionBackground": "#2563EB",
      "selectionForeground": "#FFFFFF"
    },
    "ScrollPane": {
      "background": "#1E1E1E"
    },
    "Viewport": {
      "background": "#1E1E1E"
    },
    "MenuBar": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5"
    },
    "Menu": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5"
    },
    "MenuItem": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5"
    },
    "ToolBar": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5"
    },
    "TabbedPane": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5"
    },
    "SplitPane": {
      "background": "#1E1E1E"
    }
  },
  "ui": {
    "ToolTip.background": "#252526",
    "ToolTip.foreground": "#FFFFFF",
    "ScrollBar.thumb": "#555555",
    "ScrollBar.track": "#1E1E1E",
    "Separator.foreground": "#3A3A3A"
  }
}
```

---

## Campos raiz

| Campo | Tipo | Descrição |
|---|---|---|
| `lookAndFeel` | `String` | Classe do Look and Feel a aplicar antes do tema. Opcional. |
| `font` | `String` ou objeto | Fonte global aplicada aos principais componentes Swing. |
| `colors` | objeto | Cores semânticas globais. |
| `components` | objeto | Propriedades por prefixo de componente Swing. |
| `ui` | objeto | Chaves diretas do `UIManager`. |

---

## lookAndFeel

`lookAndFeel` recebe o nome completo da classe do L&F:

```json
{
  "lookAndFeel": "javax.swing.plaf.nimbus.NimbusLookAndFeel"
}
```

Se omitido, o tema é aplicado sobre o Look and Feel atual.

---

## font

Pode ser uma string:

```json
{
  "font": "Segoe UI"
}
```

Ou um objeto:

```json
{
  "font": {
    "family": "Segoe UI",
    "size": 13,
    "style": "bold italic"
  }
}
```

Valores de `style`:

| Valor | Resultado |
|---|---|
| `plain` | Fonte normal |
| `bold` | Negrito |
| `italic` | Itálico |
| `bold italic` | Negrito e itálico |

Quando `font` é informado na raiz, ele é aplicado em chaves como:

```text
Button.font
Label.font
TextField.font
TextArea.font
ComboBox.font
Table.font
TableHeader.font
Tree.font
List.font
Menu.font
MenuItem.font
ToolTip.font
```

---

## colors

`colors` define cores semânticas globais. Elas também são salvas em `UIManager` com prefixo `SwingTools.color.`.

| Chave | UIManager principal | Uso |
|---|---|---|
| `background` | `Panel.background` | Cor base de fundo |
| `foreground` | `Label.foreground` | Cor base de texto |
| `text` | `Label.foreground` | Alias para texto |
| `primary` | `Button.background` | Cor principal |
| `accent` | `Button.select` | Cor de destaque |
| `danger` | `OptionPane.errorDialog.titlePane.background` | Cor de erro |
| `border` | `Component.borderColor` | Cor de borda |
| `selectionBackground` | `List.selectionBackground` | Fundo de seleção |
| `selectionForeground` | `List.selectionForeground` | Texto de seleção |
| `disabledForeground` | `Label.disabledForeground` | Texto desabilitado |

Além da chave principal, algumas cores são propagadas para vários componentes. Por exemplo, `background` também pode preencher:

```text
Panel.background
Viewport.background
ScrollPane.background
TextArea.background
TextPane.background
EditorPane.background
Table.background
Tree.background
List.background
Menu.background
MenuItem.background
MenuBar.background
PopupMenu.background
OptionPane.background
ToolBar.background
TabbedPane.background
SplitPane.background
```

---

## components

`components` usa o prefixo padrão do `UIManager`.

Exemplo:

```json
{
  "components": {
    "Button": {
      "background": "#2563EB",
      "foreground": "#FFFFFF"
    }
  }
}
```

Isso vira:

```java
UIManager.put("Button.background", color);
UIManager.put("Button.foreground", color);
```

### Componentes atualizados automaticamente

Ao atualizar janelas abertas, `JsonLookAndFeel` reconhece e reaplica defaults para:

| Swing | Prefixo |
|---|---|
| `JPanel` | `Panel` |
| `JLabel` | `Label` |
| `JButton` | `Button` |
| `JToggleButton` | `ToggleButton` |
| `JCheckBox` | `CheckBox` |
| `JRadioButton` | `RadioButton` |
| `JTextField` | `TextField` |
| `JTextArea` | `TextArea` |
| `JTextPane` | `TextPane` |
| `JEditorPane` | `EditorPane` |
| `JComboBox` | `ComboBox` |
| `JTable` | `Table` |
| `JTableHeader` | `TableHeader` |
| `JScrollPane` | `ScrollPane` |
| `JViewport` | `Viewport` |
| `JList` | `List` |
| `JTree` | `Tree` |
| `JMenuBar` | `MenuBar` |
| `JMenu` | `Menu` |
| `JMenuItem` | `MenuItem` |
| `JToolBar` | `ToolBar` |
| `JTabbedPane` | `TabbedPane` |
| `JSplitPane` | `SplitPane` |

### Aliases de propriedades

| JSON | Chave(s) UIManager geradas |
|---|---|
| `background` | `<Component>.background` |
| `foreground` | `<Component>.foreground` |
| `text` | `<Component>.foreground` |
| `selectionBackground` | `<Component>.selectionBackground` |
| `selectionForeground` | `<Component>.selectionForeground` |
| `disabledBackground` | `<Component>.disabledBackground` |
| `disabledForeground` | `<Component>.disabledForeground` |
| `caret` | `<Component>.caretForeground` |
| `caretForeground` | `<Component>.caretForeground` |
| `font` | `<Component>.font` |
| `border` | `<Component>.border` e `<Component>.borderColor` |
| `borderColor` | `<Component>.borderColor` |
| `padding` | `<Component>.margin` |
| `margin` | `<Component>.margin` |
| `rowHeight` | `<Component>.rowHeight` |
| `arc` | `<Component>.arc` |
| `borderRadius` | `<Component>.arc` |

Propriedades sem alias são aplicadas diretamente:

```json
{
  "components": {
    "Table": {
      "gridColor": "#E5E7EB"
    }
  }
}
```

Gera:

```java
UIManager.put("Table.gridColor", value);
```

---

## ui

`ui` permite definir qualquer chave direta do `UIManager`.

```json
{
  "ui": {
    "ToolTip.background": "#252526",
    "ToolTip.foreground": "#FFFFFF",
    "ScrollBar.thumb": "#555555",
    "Table.showGrid": true
  }
}
```

Use `ui` quando uma chave não se encaixar bem em `colors` ou `components`.

---

## Tipos aceitos

### Cores

Formatos aceitos:

| Formato | Exemplo |
|---|---|
| HEX curto | `#RGB` |
| HEX completo | `#RRGGBB` |
| HEX curto com alpha | `#RGBA` |
| HEX completo com alpha | `#RRGGBBAA` |
| RGB | `rgb(30, 30, 30)` |
| RGBA | `rgba(30, 30, 30, 0.8)` |
| Nome simples | `black`, `white`, `red`, `green`, `blue`, `gray`, `lightgray`, `darkgray`, `transparent` |

### Insets

Usado em `padding`, `margin` e chaves que contenham `inset`.

```json
{
  "padding": [8, 14]
}
```

Regras:

| Formato | Resultado |
|---|---|
| `[8]` | top/right/bottom/left = 8 |
| `[8, 14]` | vertical = 8, horizontal = 14 |
| `[4, 8, 4, 8]` | top/right/bottom/left |
| `{ "top": 4, "left": 8, "bottom": 4, "right": 8 }` | valores nomeados |

### Dimension

Usado em chaves que contenham `size` ou `dimension`.

```json
{
  "preferredSize": [320, 240]
}
```

Também aceita:

```json
{
  "preferredSize": {
    "width": 320,
    "height": 240
  }
}
```

### Font

```json
{
  "font": {
    "family": "JetBrains Mono",
    "size": 14,
    "style": "bold"
  }
}
```

---

## Tema claro completo

```json
{
  "font": {
    "family": "Segoe UI",
    "size": 13
  },
  "colors": {
    "background": "#FFFFFF",
    "foreground": "#1F2937",
    "primary": "#2563EB",
    "border": "#D1D5DB",
    "selectionBackground": "#DBEAFE",
    "selectionForeground": "#111827",
    "disabledForeground": "#9CA3AF"
  },
  "components": {
    "Panel": {
      "background": "#FFFFFF",
      "foreground": "#1F2937"
    },
    "Label": {
      "foreground": "#1F2937"
    },
    "Button": {
      "background": "#2563EB",
      "foreground": "#FFFFFF",
      "padding": [8, 14],
      "borderRadius": 8
    },
    "TextField": {
      "background": "#FFFFFF",
      "foreground": "#111827",
      "caret": "#111827",
      "selectionBackground": "#DBEAFE",
      "selectionForeground": "#111827",
      "borderColor": "#D1D5DB",
      "padding": [6, 10]
    },
    "ComboBox": {
      "background": "#FFFFFF",
      "foreground": "#111827",
      "selectionBackground": "#DBEAFE",
      "selectionForeground": "#111827"
    },
    "CheckBox": {
      "background": "#FFFFFF",
      "foreground": "#1F2937"
    },
    "RadioButton": {
      "background": "#FFFFFF",
      "foreground": "#1F2937"
    },
    "ScrollPane": {
      "background": "#FFFFFF"
    },
    "Viewport": {
      "background": "#FFFFFF"
    },
    "TableHeader": {
      "background": "#F3F4F6",
      "foreground": "#111827"
    },
    "Table": {
      "background": "#FFFFFF",
      "foreground": "#111827",
      "gridColor": "#E5E7EB",
      "rowHeight": 28,
      "selectionBackground": "#DBEAFE",
      "selectionForeground": "#111827"
    },
    "List": {
      "background": "#FFFFFF",
      "foreground": "#111827",
      "selectionBackground": "#DBEAFE",
      "selectionForeground": "#111827"
    },
    "Tree": {
      "background": "#FFFFFF",
      "foreground": "#111827",
      "selectionBackground": "#DBEAFE",
      "selectionForeground": "#111827"
    }
  },
  "ui": {
    "ToolTip.background": "#FFFFFF",
    "ToolTip.foreground": "#111827",
    "ScrollBar.thumb": "#CBD5E1",
    "Separator.foreground": "#E5E7EB"
  }
}
```

---

## Tema escuro completo

```json
{
  "font": {
    "family": "Segoe UI",
    "size": 13
  },
  "colors": {
    "background": "#1E1E1E",
    "foreground": "#F5F5F5",
    "primary": "#3B82F6",
    "border": "#3A3A3A",
    "selectionBackground": "#2563EB",
    "selectionForeground": "#FFFFFF",
    "disabledForeground": "#8A8A8A"
  },
  "components": {
    "Panel": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5"
    },
    "Label": {
      "foreground": "#F5F5F5"
    },
    "Button": {
      "background": "#3B82F6",
      "foreground": "#FFFFFF",
      "padding": [8, 14],
      "borderRadius": 8
    },
    "TextField": {
      "background": "#252526",
      "foreground": "#FFFFFF",
      "caret": "#FFFFFF",
      "selectionBackground": "#2563EB",
      "selectionForeground": "#FFFFFF",
      "borderColor": "#555555",
      "padding": [6, 10]
    },
    "ComboBox": {
      "background": "#252526",
      "foreground": "#FFFFFF",
      "selectionBackground": "#2563EB",
      "selectionForeground": "#FFFFFF"
    },
    "CheckBox": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5"
    },
    "RadioButton": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5"
    },
    "ScrollPane": {
      "background": "#1E1E1E"
    },
    "Viewport": {
      "background": "#1E1E1E"
    },
    "TableHeader": {
      "background": "#252526",
      "foreground": "#F5F5F5"
    },
    "Table": {
      "background": "#1E1E1E",
      "foreground": "#F5F5F5",
      "gridColor": "#3A3A3A",
      "rowHeight": 28,
      "selectionBackground": "#2563EB",
      "selectionForeground": "#FFFFFF"
    },
    "List": {
      "background": "#252526",
      "foreground": "#FFFFFF",
      "selectionBackground": "#2563EB",
      "selectionForeground": "#FFFFFF"
    },
    "Tree": {
      "background": "#1E1E1E",
      "foreground": "#FFFFFF",
      "selectionBackground": "#2563EB",
      "selectionForeground": "#FFFFFF"
    }
  },
  "ui": {
    "ToolTip.background": "#252526",
    "ToolTip.foreground": "#FFFFFF",
    "ScrollBar.thumb": "#555555",
    "ScrollBar.track": "#1E1E1E",
    "Separator.foreground": "#3A3A3A"
  }
}
```

---

## Exemplo de troca de tema em runtime

```java
JButton light = new JButton("Claro");
JButton dark = new JButton("Escuro");

light.addActionListener(e -> JsonLookAndFeel.apply(Path.of("light-theme.json")));
dark.addActionListener(e -> JsonLookAndFeel.apply(Path.of("dark-theme.json")));
```

`JsonLookAndFeel.apply(...)` já atualiza as janelas abertas. Se você aplicou com `false`, chame:

```java
JsonLookAndFeel.updateOpenWindows();
```

---

## Boas práticas

- Use `colors` para tokens globais e reaproveitáveis.
- Use `components` para configurar famílias de componentes.
- Use `ui` para chaves específicas do `UIManager`.
- Não é obrigatório preencher todas as chaves. O que faltar continua vindo do Look and Feel atual.
- Para componentes customizados, leia cores e fontes do `UIManager` sempre que possível.
- Para trocar tema em runtime, evite setar cores fixas diretamente em cada componente, porque isso dificulta reaplicar defaults.
