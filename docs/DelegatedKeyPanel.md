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

    public WizardPanel() {
        register("account", new AccountPanel(), true);
        register("confirm", new ConfirmPanel());
    }
}

class WizardController extends AbstractViewController<KeyPanel> {
    private boolean listenerInstalled;

    @Override
    public void onInit(KeyPanel component) {
        super.onInit(component);
        if (listenerInstalled) return;
        component.addEventListener(EventType.BEFORE_CHANGE, event -> {
            KeyPanelContextChangeEvent change = event.tryGetValue();
            if (!canLeaveCurrentStep()) {
                change.cancel();
            }
        });
        listenerInstalled = true;
    }
}
```

Registre as telas uma vez, no construtor. `onLoad()` pode ocorrer novamente quando o painel volta a ficar visível; registrar as telas ou listeners a cada exibição duplicaria o fluxo.

## Cuidados

- O `KeyPanel` gerencia seu proprio layout; configure o layout dos paineis registrados.
- Use chaves estaveis para cada etapa.
- Use eventos `BEFORE_CHANGE` e `CHANGE` para validacao e rastreio de navegacao.
