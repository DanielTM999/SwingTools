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
public class UserFormPanel extends DelegatedBlockingPanel<UserFormController> {
    @Override
    protected UserFormController newController() {
        return new UserFormController();
    }

    @Override
    protected void onInit() {
        setLayout(new BorderLayout());
        JTextField name = new JTextField();
        name.setName("name");
        add(name, BorderLayout.NORTH);
    }
}

class UserFormController extends BindingAbstractViewController<BlockingPanel> {
    @ViewRef("name")
    private JTextField name;

    @Override
    public void onLoad(BlockingPanel component) {
        name.addActionListener(e -> System.out.println(name.getText()));
    }
}
```

## Cuidados

- Use `BindingAbstractViewController` quando precisar de `@ViewRef` ou `@ClientRef`.
- Chame `disposeController()` se remover o painel manualmente e quiser liberar a referencia antes do GC.
- O controller deve manipular UI na EDT.
