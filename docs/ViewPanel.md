# ViewPanel

`ViewPanel` e a classe base para criar views reutilizaveis dentro do SwingTools.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component` |
| Heranca | `ViewPanel extends JPanel implements IWindowComponent` |
| Uso principal | Raiz de telas, fragments, formularios e blocos visuais reutilizaveis |

## Papel na arquitetura

Use `ViewPanel` quando voce quer um `JPanel` com ciclo de vida, DOM local e estado client-side. Ele nao substitui o Swing: todos os metodos de `JPanel` continuam disponiveis.

```text
JPanel
  ViewPanel
    BlockingPanel
      PanelEventListener
```

## Ciclo de vida

| Metodo | Quando usar |
|---|---|
| `onInit()` | Configuracoes iniciais do painel |
| `onLoad()` | Depois que o componente entra na hierarquia Swing |
| `onRemoved()` | Quando o componente sai da hierarquia |
| `onDrawing()` | Pintura customizada, chamada em `paintComponent` |
| `onFocus(FocusEvent)` / `onLostFocus(FocusEvent)` | Foco do painel |
| `onClick(MouseEvent)` | Clique, quando o listener interno estiver habilitado |
| `onResize()` / `onMove()` / `onShow()` / `onHidden()` | Eventos de componente |

## DOM local

`ViewPanel` indexa filhos pelo `setName`.

```java
JButton save = new JButton("Salvar");
save.setName("saveButton");
add(save);

reloadDomElements();
JButton ref = findById("saveButton");
```

Chame `reloadDomElements()` quando adicionar ou remover filhos dinamicamente e precisar encontra-los por nome.

## Estado client-side

```java
putInClient("selectedUserId", 42L);
Long id = getFromClient("selectedUserId", -1L);
```

Esse estado pertence ao painel. Use para dados temporarios de UI, nao para persistencia.

## Exemplo recomendado

```java
public class SearchView extends ViewPanel {
    private final JTextField input = new JTextField();

    @Override
    protected void onInit() {
        setLayout(new BorderLayout(8, 8));
        input.setName("searchInput");
        add(input, BorderLayout.NORTH);
    }

    @Override
    protected void onLoad() {
        input.addActionListener(e -> putInClient("lastQuery", input.getText()));
        reloadDomElements();
    }
}
```

## Cuidados

- Monte a estrutura visual em `onInit` ou no construtor; conecte comportamento em `onLoad`.
- Alteracoes de UI vindas de threads externas devem usar `runOnUiTread`.
- Se a view precisar bloquear interacao, estenda `BlockingPanel`.
- Se a view precisar emitir eventos publicos, estenda `PanelEventListener`.
