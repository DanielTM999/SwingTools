# TitleMenuBar

`TitleMenuBar` cria uma barra de titulo customizada integrada a `MenuBar`.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.window` |
| Heranca | `TitleMenuBar extends JPanel` |
| Uso principal | Janela com titulo/menu customizado e area trailing/central |

## Instalacao

```java
MenuBar menu = new MenuBar();
menu.menu("file", "Arquivo", file -> {
    file.addItem("exit", "Sair");
});
TitleMenuBar title = TitleMenuBar.install(frame, menu)
        .titleBarHeight(36)
        .gradient(new Color(0x111827), new Color(0x374151));
```

| Metodo | Uso |
|---|---|
| `install(JFrame, MenuBar)` | Cria e instala |
| `installOn(JFrame)` | Instala instancia atual |
| `configureRootPane(JRootPane)` | Ajusta root pane |
| `configureLookAndFeelDefaults(Color, Color)` | Defaults de LAF |
| `configureButtonHeight(int)` | Altura dos botoes |

## Layout e visual

| Metodo | Uso |
|---|---|
| `gradient(...)` | Fundo em gradiente |
| `background(Color)` / `foreground(Color)` | Cores |
| `gradientAngle(double)` | Angulo |
| `gradientIntensity(float)` | Intensidade |
| `titleBarHeight(int)` | Altura |
| `addTrailing(Component)` | Area direita |
| `center(Component)` / `clearCenter()` | Conteudo central |
| `getMenuBar()` | Menu associado |

## Cuidados

- Use com `JFrame`; para dialogs, valide comportamento do root pane antes.
- Se trocar Look and Feel, reaplique defaults ou atualize a arvore de componentes.
