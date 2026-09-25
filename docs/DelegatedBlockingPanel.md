# DelegatedBlockingPanel

`DelegatedBlockingPanel<T>` e um `BlockingPanel` com controller associado.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.delegated` |
| Heranca | `DelegatedBlockingPanel<T extends AbstractViewController<BlockingPanel>> extends BlockingPanel` |
| Contrato extra | `DelegatedIWindowComponent` |
| Uso principal | Painel reutilizavel com comportamento separado em controller |

## Como funciona

Ao entrar na hierarquia Swing, o painel cria/associa o controller retornado por `newController()` e encaminha ciclo de vida:

| Evento do painel | Metodo do controller |
|---|---|
| Inicializacao | `onInit(component)` |
| `onLoad()` | `onLoad(component)` |
| `onRemoved()` | `onRemoved(component)` |
| foco | `onFocus(component)` / `onLostFocus(component)` |

## Exemplo

```java
import dtm.stools.component.delegated.DelegatedBlockingPanel;
import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.context.annotations.ViewRef;
import dtm.stools.controllers.component.BindingAbstractViewController;

import javax.swing.JTextField;
import java.awt.BorderLayout;

public class UserFormPanel extends DelegatedBlockingPanel<UserFormController> {
    public UserFormPanel() {
        applyDrawingOnce();
    }

    @Override
    protected UserFormController newController() {
        return new UserFormController();
    }

    @Override
    protected void onDrawing() {
        super.onDrawing();
        setLayout(new BorderLayout());
        JTextField name = new JTextField();
        name.setName("name");
        add(name, BorderLayout.NORTH);
    }
}

class UserFormController extends BindingAbstractViewController<BlockingPanel> {
    @ViewRef("name")
    private JTextField name;
    private boolean listenerInstalled;

    @Override
    public void onInit(BlockingPanel component) {
        super.onInit(component);
        if (listenerInstalled) return;
        name.addActionListener(e -> System.out.println(name.getText()));
        listenerInstalled = true;
    }
}
```

O painel monta o campo antes da recarga do índice de componentes. O binding do controller acontece em `onInit`, depois dessa recarga. `applyDrawingOnce()` e a guarda do listener evitam duplicação quando o painel é retirado e reinserido na hierarquia.

## Cuidados

- Use `BindingAbstractViewController` quando precisar de `@ViewRef` ou `@ClientRef`.
- Chame `disposeController()` se remover o painel manualmente e quiser liberar a referencia antes do GC.
- O controller deve manipular UI na EDT.
