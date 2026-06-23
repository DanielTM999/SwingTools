# KeyPanel

`KeyPanel` e um container para registrar varios `JPanel`s por chave e mostrar apenas um por vez.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels` |
| Heranca | `KeyPanel extends PanelEventListener` |
| Base Swing | `JPanel` via `ViewPanel`/`BlockingPanel` |
| Uso principal | Navegacao interna entre telas, steps, estados ou fragments |

## Heranca e comportamento

```text
ViewPanel
  BlockingPanel
    PanelEventListener
      KeyPanel
```

Por herdar `PanelEventListener`, `KeyPanel` tambem tem `lockUI`, `unlockUI`, `addEventListner`, estado client-side e DOM local.

`KeyPanel` gerencia seu proprio layout. Nao chame `setLayout` com outro layout; configure o layout dos paineis registrados.

## Registro de paineis

| Metodo | Contrato |
|---|---|
| `register(String key, JPanel panel)` | Registra o painel sem trocar para ele |
| `register(String key, JPanel panel, boolean call)` | Registra e, se `call=true`, troca para ele |
| `register(JPanel panel)` | Registra com chave gerada e retorna a chave |
| `unregister(String key)` | Remove por chave |
| `unregister(JPanel panel)` | Remove por instancia |
| `unregisterAll()` | Remove todos |

## Navegacao

| Metodo | Contrato |
|---|---|
| `switchTo(String key)` | Mostra o painel da chave |
| `switchTo(JPanel panel)` | Mostra o painel pela instancia |
| `switchFirst()` / `switchLast()` | Vai para primeiro ou ultimo painel registrado |
| `switchNext()` / `switchPrevious()` | Navega pela ordem de registro |
| `switchToLastPanel()` | Volta para o painel anterior |

## Consulta de estado

| Metodo | Retorno |
|---|---|
| `getCurrent()` | Painel atual |
| `getCurrentKey()` | Chave atual |
| `find(String key)` | Painel registrado |
| `contains(String key)` / `contains(JPanel)` | Existencia |
| `getKeyOf(JPanel)` | Chave de uma instancia |
| `getKeys()` / `getPanels()` | Colecoes registradas |
| `getSizePanel()` / `isEmpty()` | Tamanho |

## Eventos

`KeyPanel` dispara:

| Evento | Quando ocorre | Payload |
|---|---|---|
| `EventType.BEFORE_CHANGE` | Antes de trocar de painel | `KeyPanelContextChangeEvent`, cancelavel |
| `EventType.CHANGE` | Depois da troca | `KeyPanelContextChangeEvent` |
| `EventType.RESIZE` | Quando o layout reposiciona o painel atual | `Dimension` |

Exemplo com cancelamento:

```java
keyPanel.addEventListner(EventType.BEFORE_CHANGE, event -> {
    KeyPanelContextChangeEvent change = event.tryGetValue();
    if ("admin".equals(change.getKey()) && !userCanOpenAdmin()) {
        change.cancel();
    }
});
```

## Exemplo completo

```java
KeyPanel pages = new KeyPanel();

JPanel list = new JPanel(new BorderLayout());
list.add(new JLabel("Lista"), BorderLayout.CENTER);

JPanel form = new JPanel(new BorderLayout());
form.add(new JLabel("Formulario"), BorderLayout.CENTER);

pages.register("list", list, true);
pages.register("form", form);

JButton next = new JButton("Editar");
next.addActionListener(e -> pages.switchTo("form"));
```

## Animacao

Use `setAnimator(ComponentAnimator<JPanel>)` para controlar a transicao. O animator recebe painel atual, proximo painel e callback de conclusao. Sempre chame o callback ao final para o `KeyPanel` finalizar a troca.
