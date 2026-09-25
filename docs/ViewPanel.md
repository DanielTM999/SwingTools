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
| `onDrawing()` | Montagem dos filhos quando o painel entra na hierarquia; pode repetir após remoção e reinserção |
| `onInit()` | Depois da montagem e da recarga do índice de componentes, em `addNotify()` |
| `onLoad()` | Quando o painel passa a estar visível; pode repetir ao ocultar e exibir |
| `onRemoved()` | Quando deixa de estar visível ou é removido da hierarquia |
| `onFocus(FocusEvent)` / `onLostFocus(FocusEvent)` | Foco do painel |
| `onClick(MouseEvent)` | Clique, quando o listener interno estiver habilitado |
| `onResize()` / `onMove()` / `onShow()` / `onHidden()` | Eventos de componente |

`onDrawing()` **não é** um callback de pintura por frame. Para desenho personalizado, sobrescreva `paintComponent(Graphics)` e chame `super.paintComponent(g)`. Se a estrutura deve ser montada apenas uma vez, chame `applyDrawingOnce()` no construtor. Como `onLoad()` pode ser chamado novamente, evite registrar o mesmo listener nele a cada exibição.

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
import dtm.stools.component.ViewPanel;

import javax.swing.JTextField;
import java.awt.BorderLayout;

public class SearchView extends ViewPanel {
    private final JTextField input = new JTextField();

    public SearchView() {
        applyDrawingOnce();
    }

    @Override
    protected void onDrawing() {
        super.onDrawing();
        setLayout(new BorderLayout(8, 8));
        input.setName("searchInput");
        add(input, BorderLayout.NORTH);
        input.addActionListener(e -> putInClient("lastQuery", input.getText(), true));
    }

    @Override
    protected void onLoad() {
        String lastQuery = getFromClient("lastQuery", "");
        input.setText(lastQuery);
    }
}
```

## Cuidados

- Monte os filhos em `onDrawing()` com `applyDrawingOnce()` quando a view puder ser reinserida. Use `onLoad()` para atualizar estado visível, sem duplicar listeners.
- `putInClient(key, value)` não substitui uma chave existente; use `putInClient(key, value, true)` para atualizá-la.
- Alteracoes de UI vindas de threads externas devem usar `runOnUiTread`.
- Se a view precisar bloquear interacao, estenda `BlockingPanel`.
- Se a view precisar emitir eventos publicos, estenda `PanelEventListener`.
