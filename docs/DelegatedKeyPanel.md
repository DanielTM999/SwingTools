# DelegatedKeyPanel

`DelegatedKeyPanel<T>` e a versao de `KeyPanel` com controller delegado.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.delegated` |
| Heranca | `DelegatedKeyPanel<T extends AbstractViewController<KeyPanel>> extends KeyPanel` |
| Contrato extra | `DelegatedIWindowComponent` |
| Uso principal | Fluxos internos com varias telas e controller dedicado |

## Quando usar

Use quando o painel controla navegacao interna e a regra de troca de telas nao deve ficar acoplada na view. Exemplos: wizard, cadastro em etapas, workspace com paineis internos, fluxo de login/recuperacao.

## Exemplo

```java
public class WizardPanel extends DelegatedKeyPanel<WizardController> {
    @Override
    protected WizardController newController() {
        return new WizardController();
    }

    @Override
    protected void onLoad() {
        register("account", new AccountPanel(), true);
        register("confirm", new ConfirmPanel());
    }
}

class WizardController extends AbstractViewController<KeyPanel> {
    @Override
    public void onLoad(KeyPanel component) {
        component.addEventListner(EventType.BEFORE_CHANGE, event -> {
            KeyPanelContextChangeEvent change = event.tryGetValue();
            if (!canLeaveCurrentStep()) {
                change.cancel();
            }
        });
    }
}
```

## Cuidados

- O `KeyPanel` gerencia seu proprio layout; configure o layout dos paineis registrados.
- Use chaves estaveis para cada etapa.
- Use eventos `BEFORE_CHANGE` e `CHANGE` para validacao e rastreio de navegacao.
