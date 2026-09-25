# Guia do Desenvolvedor

Este guia mostra como montar uma aplicação com SwingTools 1.3.0. Swing continua sendo a base; a biblioteca acrescenta ciclo de vida para janelas e views, controllers, eventos e componentes prontos. Comece pela janela mínima, execute-a e avance para composição de telas e extensões. O [índice](README.md) leva à página de cada componente.

## Dependencia e ambiente

Use JDK 25 ou superior e Maven. Para consumir a versão publicada no JitPack, acrescente ao `pom.xml` da aplicação:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.DanielTM999</groupId>
        <artifactId>SwingTools</artifactId>
        <version>1.3.0</version>
    </dependency>
</dependencies>
```

Para trabalhar com um checkout local da biblioteca, execute nele:

```bash
mvn clean install -Dnative.build.skip=true
```

Nesse caso, a aplicação deve usar `dtm.stools:SwingTools:1.3.0` em vez das coordenadas JitPack. Veja os detalhes de build e nativos no [README principal](../README.md#3-instalação-como-dependência).

## Arquitetura em camadas

| Camada | Classes principais | Responsabilidade |
|---|---|---|
| Janela | `Activity`, `DialogActivity`, `FragmentActivity`, `TransientPopupActivity` | Ciclo de vida, DOM por `setName`, estado client-side e execucao por janela |
| Controller | `AbstractWindowController`, `BindingAbstractWindowController` | Separar comportamento da janela e fazer binding com `@ViewRef`/`@ClientRef` |
| View | `ViewPanel`, `BlockingPanel`, `KeyPanel` | Componentes reutilizaveis com ciclo de vida local |
| Eventos | `EventListenerComponent`, `PanelEventListener`, `DataTableListener` | Eventos tipados por string, payload e propriedades extras |
| Componentes | `TabbedPanel`, `DockPanel`, `TreeView`, `GridView`, `CodeEditor`, `WordEditor`, `SheetEditor` | Área de trabalho, dados e editores especializados sobre Swing |
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
    public UserFormView() {
        applyDrawingOnce();
    }

    @Override
    protected void onDrawing() {
        super.onDrawing();
        setLayout(new BorderLayout());
        JButton save = new JButton("Salvar");
        save.setName("save");
        add(save, BorderLayout.SOUTH);
        save.addActionListener(event -> System.out.println("Salvar"));
    }
}
```

`ViewPanel` chama `onDrawing()` quando entra na hierarquia; `applyDrawingOnce()` evita recriar os filhos se o painel for removido e adicionado novamente. O índice de componentes é recarregado antes de `onInit()`. `onLoad()` acompanha a exibição e pode ocorrer várias vezes, então não registre listeners nele sem controlar duplicações. Veja [ViewPanel](ViewPanel.md).

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

Use para estado local de UI, como filtros, selecao atual e dados temporarios. A forma `putInClient(key, value)` só grava uma chave ausente; para atualizar uma chave existente, use `putInClient(key, value, true)`. Evite usar esse mapa como armazenamento global ou persistência.

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
component.addEventListener(EventType.CHANGE, event -> {
    Object value = event.getValue();
});
```

`addEventListener` retorna uma inscrição que pode ser encerrada com `unsubscribe()`. Algumas chamadas de remoção e consulta ainda preservam a grafia histórica `Listner`. Veja [Eventos.md](Eventos.md) para payload, propriedades, cancelamento e ciclo de vida das inscrições.

## Escolhendo o componente certo

| Necessidade | Use |
|---|---|
| Trocar telas internas por chave | `KeyPanel` |
| Desabilitar interacao durante carregamento | `BlockingPanel` |
| Abas de documento, editor ou workspace | `TabbedPanel` |
| Layout tipo IDE com areas laterais | `DockPanel` |
| Tabela de POJO anotado | `GridView<T>` |
| Arvore hierarquica de dominio | `TreeView<T>` |
| Campo com mascara | `MaskedTextField` |
| Valor monetario | `CurrencyField` |
| Autocomplete/busca | `SearchTextField<T>` |
| Editor de codigo extensivel | `CodeEditor` |
| Editor de documentos paginados e DOCX | `WordEditor` |
| Planilha com fórmulas, gráficos e arquivos XLSX/ODS/CSV | `SheetEditor` |
| Popup menu fluente | `ActionPopupMenu` |

## Threading

Swing exige que alterações de UI aconteçam na EDT. Crie a janela com `SwingUtilities.invokeLater`, como no exemplo inicial. `Activity.init()` executa a montagem na thread que o chamou; por isso, invoque `init()` na EDT. Para atualizar a janela depois de uma tarefa de trabalho, use `runOnUi` ou `SwingUtilities.invokeLater`. Para views, a API pública se chama `runOnUiTread` (grafia preservada no código).

```java
runOnUi(window -> {
    JButton button = window.findById("saveButton");
    button.setEnabled(true);
});
```

Para tarefas associadas a uma janela, `runOnWindowExecutor` nas activities executa trabalho em executor dedicado. Depois volte para a EDT antes de mexer em componentes. Não faça I/O demorado dentro de `onDrawing`, `onLoad` ou listeners Swing.

```java
// Dentro de uma subclasse de Activity:
runOnWindowExecutor(() -> {
    String resultado = carregarDados(); // trabalho fora da EDT
    runOnUi((Activity janela) -> {
        JTextField campo = janela.findById("resultado");
        campo.setText(resultado);
    });
});
```

O método `carregarDados()` representa a operação de sua aplicação. O código acima é um trecho para usar dentro de uma `Activity`; importe `dtm.stools.activity.Activity` e `javax.swing.JTextField`.

## Roteiros por tarefa

### Formulário com validação

1. Escolha os campos adequados em [inputs](README.md#inputs), por exemplo [NumberField](NumberField.md) para números com precisão e [MaskedTextField](MaskedTextField.md) para texto formatado.
2. Monte os campos em [FormPanel](FormPanel.md) quando precisar de rótulos, mensagens de erro e validação em bloco.
3. Leia e confirme os valores no evento de submit. Use os eventos `INPUT` para feedback durante a edição e `CHANGE` para alterações confirmadas, conforme o contrato do campo.
4. Abra [ModernDialog](ModernDialog.md) ou [ModernInputDialog](ModernInputDialog.md) se a tarefa exigir confirmação ou entrada pontual.

### Área de trabalho com abas

1. Use [TabbedPanel](TabbedPanel.md) para documentos ou views em abas. Dê uma chave estável a cada aba.
2. Use [DockPanel](DockPanel.md) se a aplicação precisa reposicionar regiões inteiras da área de trabalho. O [WindowPanel](WindowPanel.md) cobre janelas internas.
3. Ao fechar uma aba ou dock, prefira as operações `close...` quando precisar respeitar regras de fechamento e eventos `BEFORE_*`.
4. Consulte o [exemplo completo no README](../README.md#exemplo-completo) para ver árvore, editor, tabela, abas e dock na mesma janela.

### Editores especializados

- [CodeEditor](CodeEditor.md) oferece buffer de texto, busca, gutter e providers; comece com `new CodeEditor(texto)` antes de configurar extensões.
- [SheetEditor](SheetEditor.md) oferece uma planilha completa; a página separa API direta, modelo e providers. Os [contratos](SheetEditor_Contratos.md) são necessários apenas para substituições e extensões.
- [WordEditor](WordEditor.md) oferece edição rica, ribbon, navegação, paginação, DOCX no subconjunto suportado e exportação HTML/texto. A página detalha integração, arquivos assíncronos, extensões e os limites de compatibilidade DOCX.

### Aparência e recursos nativos

- Aplique [JsonLookAndFeel](JsonLookAndFeel.md) antes de criar as janelas quando quiser um tema inicial comum; use [UiTokens](UiTokens.md) nos componentes desenhados pela aplicação.
- Para seleção de arquivos, comece com [FilePickerInputPanel](FilePickerInputPanel.md). Use [OsFilePicker](OsFilePicker.md) quando precisar chamar diretamente o seletor do sistema operacional.
- Os painéis gráficos têm ciclo de vida e regras de thread próprios. Leia [Graphics](Graphics.md) antes de integrar [GraphicsGlPanel](GraphicsGlPanel.md).

## Boas praticas

- De nomes estaveis aos componentes que precisam de binding: `saveButton`, `usersTable`, `filterField`.
- Nao sobrescreva `setLayout` de componentes que gerenciam layout internamente, como `KeyPanel`.
- Prefira `closeTab`/`closeDock` quando quiser respeitar eventos e regras; use `removeTab`/`removeDock` para remocao direta.
- Evite regras de negocio dentro de componentes visuais grandes; use controllers.
- Para componentes complexos, primeiro use a API de alto nivel; depois personalize providers, renderers, factories ou styles.
- Ao atualizar docs ou exemplos, compile com `mvn -DskipTests -Dnative.build.skip=true compile`.
