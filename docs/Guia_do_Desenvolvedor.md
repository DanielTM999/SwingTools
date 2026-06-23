# Guia do Desenvolvedor

Este guia explica como pensar o SwingTools do ponto de vista de quem vai programar com a biblioteca. A ideia central e simples: Swing continua sendo a base, mas o projeto adiciona uma camada de organizacao para janelas, views, controllers, eventos e componentes prontos.

## Dependencia e ambiente

O projeto usa Java 21, Maven, Swing, FlatLaf, Lombok e Jackson. Como dependencia Maven:

```xml
<dependency>
    <groupId>dtm.stools</groupId>
    <artifactId>SwingTools</artifactId>
    <version>1.0.1</version>
</dependency>
```

Para usar a biblioteca localmente em outro projeto:

```bash
mvn clean install -Dnative.build.skip=true
```

## Arquitetura em camadas

| Camada | Classes principais | Responsabilidade |
|---|---|---|
| Janela | `Activity`, `DialogActivity`, `FragmentActivity`, `TransientPopupActivity` | Ciclo de vida, DOM por `setName`, estado client-side e execucao por janela |
| Controller | `AbstractWindowController`, `BindingAbstractWindowController` | Separar comportamento da janela e fazer binding com `@ViewRef`/`@ClientRef` |
| View | `ViewPanel`, `BlockingPanel`, `KeyPanel` | Componentes reutilizaveis com ciclo de vida local |
| Eventos | `EventListenerComponent`, `PanelEventListener`, `DataTableListener` | Eventos tipados por string, payload e propriedades extras |
| Componentes | `TabbedPanel`, `DockPanel`, `TreeView`, `GridViewTable`, `CodeEditor` | Widgets de alto nivel sobre Swing |
| Infra | `JsonLookAndFeel`, `FlexBoxLayout`, `OsFilePicker`, utils | Aparencia, layout, recursos e integracao nativa |

## Heranca das janelas

`IWindow` e o contrato comum para janelas. Ele define `init`, `requestClose`, `dispose`, `findById`, `findAllById`, `putInClient`, `getFromClient`, `reloadDomElements`, `runOnUi` e `getWindowExecutor`.

```text
IWindow
  Activity extends JFrame
  DialogActivity extends JDialog
  FragmentActivity extends JDialog
  TransientPopupActivity extends JWindow
    NotificationActivity extends TransientPopupActivity
```

Use `Activity` para janela principal, `DialogActivity` para dialogs modal ou nao modal, `FragmentActivity` para fluxos auxiliares associados a uma janela, `TransientPopupActivity` para popups temporarios e `NotificationActivity` para notificacoes empilhadas.

Exemplo minimo:

```java
import dtm.stools.activity.Activity;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.WindowEvent;

public class MainWindow extends Activity {
    private final JButton saveButton = new JButton("Salvar");

    public MainWindow() {
        super("Minha aplicacao");
    }

    @Override
    protected void onDrawing() {
        setLayout(new BorderLayout());
        saveButton.setName("saveButton");
        add(saveButton, BorderLayout.SOUTH);
        setSize(800, 500);
        setLocationRelativeTo(null);
    }

    @Override
    protected void onLoad(WindowEvent e) {
        saveButton.addActionListener(event -> System.out.println("Salvar"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().init());
    }
}
```

## Ciclo de vida

O ciclo esperado e:

1. Construtor: guarde dependencias e configure valores simples.
2. `init()`: chamado pelo usuario para inicializar a janela.
3. `onDrawing()`: monte a arvore Swing e defina layout, componentes, nomes e tamanho.
4. `reloadDomElements()`: a janela indexa componentes nomeados.
5. `onLoad(...)`: conecte listeners, carregue dados e dispare tarefas iniciais.
6. Eventos de janela: `onResize`, `onMove`, `onShow`, `onHidden`, `onFocus`, `onLostFocus`.
7. Fechamento: use `requestClose()` quando quiser respeitar `onClose`; `dispose()` deve ser o descarte final.

Regra pratica: crie componente em `onDrawing`; conecte comportamento em `onLoad`. Isso evita buscar via `findById` antes de o DOM existir.

## Heranca dos componentes

`IWindowComponent` e a versao de `IWindow` para componentes: DOM local, estado client-side e execucao na EDT.

```text
IWindowComponent
  ViewPanel extends JPanel
    BlockingPanel
      PanelEventListener
        KeyPanel
        TabbedPanel
        DockPanel
        SwitchField
```

`ViewPanel` e uma view reutilizavel. `BlockingPanel` adiciona bloqueio de interacao. `PanelEventListener` adiciona eventos. Componentes como `KeyPanel`, `TabbedPanel`, `DockPanel` e `SwitchField` herdam essa base.

Exemplo de view:

```java
import dtm.stools.component.ViewPanel;

import javax.swing.JButton;
import java.awt.BorderLayout;

public class UserFormView extends ViewPanel {
    @Override
    protected void onInit() {
        setLayout(new BorderLayout());
    }

    @Override
    protected void onLoad() {
        JButton save = new JButton("Salvar");
        save.setName("save");
        add(save, BorderLayout.SOUTH);
        reloadDomElements();
    }
}
```

## DOM por `setName`

`findById` e `findAllById` procuram componentes pelo valor de `Component#setName`.

```java
JButton save = new JButton("Salvar");
save.setName("saveButton");

JButton ref = findById("saveButton");
```

Se componentes forem adicionados depois do carregamento inicial, chame `reloadDomElements()` antes de buscar ou antes de depender de binding.

## Client state

`putInClient` e `getFromClient` guardam estado associado a uma janela ou view.

```java
putInClient("userId", 10L);
Long userId = getFromClient("userId");
```

Use para estado local de UI, como filtros, selecao atual e dados temporarios. Evite usar como armazenamento global ou cache de longa duracao.

## Controllers delegados

Quando uma janela ou painel comeca a ter regra demais, use controller. As classes `DelegatedActivity`, `DelegatedDialogActivity`, `DelegatedBlockingPanel` e `DelegatedKeyPanel` criam o controller e encaminham eventos de ciclo de vida.

```java
import dtm.stools.activity.delegated.DelegatedActivity;
import dtm.stools.controllers.BindingAbstractWindowController;
import dtm.stools.context.annotations.ViewRef;

import javax.swing.JButton;
import java.awt.BorderLayout;

public class UsersWindow extends DelegatedActivity<UsersController> {
    @Override
    protected UsersController newController() {
        return new UsersController();
    }

    @Override
    protected void onDrawing() {
        JButton reload = new JButton("Recarregar");
        reload.setName("reload");
        add(reload, BorderLayout.NORTH);
        setSize(600, 400);
    }
}

class UsersController extends BindingAbstractWindowController<dtm.stools.activity.Activity> {
    @ViewRef("reload")
    private JButton reloadButton;

    @Override
    public void onLoad(dtm.stools.activity.Activity window) {
        reloadButton.addActionListener(e -> System.out.println("reload"));
    }
}
```

`@ViewRef` injeta componente encontrado por `setName`. Se o campo chama `reload` e a anotacao nao tiver valor, o nome do campo e usado. `@ClientRef` injeta valores guardados com `putInClient`.

## Eventos do SwingTools

Componentes que implementam `EventListenerComponent` usam:

```java
component.addEventListner(EventType.CHANGE, event -> {
    Object value = event.getValue();
});
```

A grafia publica da API e `Listner`, sem o segundo `e`, porque e assim que esta no codigo. Veja [Eventos.md](Eventos.md) para detalhes de payload, propriedades e cancelamento.

## Escolhendo o componente certo

| Necessidade | Use |
|---|---|
| Trocar telas internas por chave | `KeyPanel` |
| Desabilitar interacao durante carregamento | `BlockingPanel` |
| Abas de documento, editor ou workspace | `TabbedPanel` |
| Layout tipo IDE com areas laterais | `DockPanel` |
| Tabela de POJO anotado | `GridViewTable<T>` |
| Arvore hierarquica de dominio | `TreeView<T>` |
| Campo com mascara | `MaskedTextField` |
| Valor monetario | `CurrencyField` |
| Autocomplete/busca | `SearchTextField<T>` |
| Editor de codigo extensivel | `CodeEditor` |
| Popup menu fluente | `ActionPopupMenu` |

## Threading

Swing exige que alteracoes de UI acontecam na EDT. Use `SwingUtilities.invokeLater`, `IWindow#runOnUi` ou `IWindowComponent#runOnUiTread` quando vier de uma thread de trabalho.

```java
runOnUi(window -> {
    JButton button = window.findById("saveButton");
    button.setEnabled(true);
});
```

Para tarefas associadas a uma janela, `runOnWindowExecutor` nas activities executa trabalho em executor dedicado. Depois volte para a EDT antes de mexer em componentes.

## Boas praticas

- De nomes estaveis aos componentes que precisam de binding: `saveButton`, `usersTable`, `filterField`.
- Nao sobrescreva `setLayout` de componentes que gerenciam layout internamente, como `KeyPanel`.
- Prefira `closeTab`/`closeDock` quando quiser respeitar eventos e regras; use `removeTab`/`removeDock` para remocao direta.
- Evite regras de negocio dentro de componentes visuais grandes; use controllers.
- Para componentes complexos, primeiro use a API de alto nivel; depois personalize providers, renderers, factories ou styles.
- Ao atualizar docs ou exemplos, compile com `mvn -DskipTests -Dnative.build.skip=true compile`.
