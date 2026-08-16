# SwingTools

SwingTools é uma biblioteca Java Swing para criar aplicações desktop com uma camada de organização acima do Swing puro. O projeto reúne ciclo de vida de janelas, controllers, binding por anotações, componentes visuais reutilizáveis, sistema de eventos, layouts, menus, docking, abas, file picker nativo, dialogs modernos, notificações e um editor de código extensível.

O foco do projeto é reduzir código repetitivo em aplicações Swing e oferecer componentes prontos para interfaces desktop mais completas.

**Stack principal:** Java 21, Maven, Swing, FlatLaf, Lombok e Jackson.

---

## Sumário

1. [O que o projeto entrega](#1-o-que-o-projeto-entrega)
2. [Requisitos](#2-requisitos)
3. [Instalação como dependência](#3-instalação-como-dependência)
4. [Build local](#4-build-local)
5. [Estrutura do repositório](#5-estrutura-do-repositório)
6. [Arquitetura geral](#6-arquitetura-geral)
7. [Janelas, activities e ciclo de vida](#7-janelas-activities-e-ciclo-de-vida)
8. [Controllers e binding](#8-controllers-e-binding)
9. [Estado, DOM e navegação](#9-estado-dom-e-navegação)
10. [Sistema de eventos](#10-sistema-de-eventos)
11. [Componentes base](#11-componentes-base)
12. [Menus](#12-menus)
13. [Abas e docking](#13-abas-e-docking)
14. [Campos de entrada](#14-campos-de-entrada)
15. [File pickers](#15-file-pickers)
16. [GridViewTable](#16-gridviewtable)
17. [TreeView](#17-treeview)
18. [CodeEditor](#18-codeeditor)
19. [GraphicsPanel e GraphicsGlPanel](#19-graphicspanel-e-graphicsglpanel)
20. [Dialogs, popups e notificações](#20-dialogs-popups-e-notificações)
21. [FlexBoxLayout](#21-flexboxlayout)
22. [JsonLookAndFeel](#22-jsonlookandfeel)
23. [System tray](#23-system-tray)
24. [Utilitários](#24-utilitários)
25. [Recursos nativos](#25-recursos-nativos)
26. [Exemplos disponíveis](#26-exemplos-disponíveis)
27. [CI e empacotamento](#27-ci-e-empacotamento)
28. [Status e observações](#28-status-e-observações)

---

## 1. O que o projeto entrega

SwingTools cobre vários pontos comuns de uma aplicação desktop:

| Área | Recursos |
|---|---|
| Janela | `Activity`, `DialogActivity`, `FragmentActivity`, `TransientPopupActivity`, `NotificationActivity` |
| Ciclo de vida | `init`, `onDrawing`, `onLoad`, `onClose`, foco, resize, show/hide, erro e system tray |
| Controller | Controllers para janelas e componentes, incluindo binding com `@ViewRef` e `@ClientRef` |
| Estado | Mapa client-side por janela e pilha global de janelas via `WindowContext` |
| DOM Swing | Busca de componentes por `setName(...)` com `findById` e `findAllById` |
| Eventos | Eventos tipados para componentes próprios e painéis |
| Layout | `FlexBoxLayout`, inspirado em CSS Flexbox |
| Menus | `MenuBar`, `CollapsibleMenuBar`, schema/configuração e `ActionPopupMenu` |
| Abas | `TabbedPanel`, header customizável, pin, dirty state, badge, menu, drag e split |
| Docking | `DockPanel` com regiões, movimentação, políticas de drop e snapshot de layout |
| Formulários | Text fields, máscaras, busca, path field, dropdown, switch, tags, cor, data e arquivo |
| Tabelas | `GridViewTable` com reflexão, anotações, seleção, edição e paginação |
| Árvore | `TreeView` com checkbox, lazy load, busca, filtro, edição, popup e drag and drop |
| Editor | `CodeEditor` com gutter, minimap, busca, folding, markers, providers e extensões |
| Gráficos | `AbstractGraphicsPanel` e `GraphicsGlPanel` com renderer, loop, FPS, input e OpenGL nativo |
| Feedback | Dialogs modernos, input dialog, popups, toasts e notificações empilháveis |
| Tema | `JsonLookAndFeel` para aplicar tema por JSON |
| Nativo | File picker nativo e suporte OpenGL via bibliotecas JNI |

---

## 2. Requisitos

| Item | Versão/observação |
|---|---|
| Java | 21 ou superior |
| Build | Maven |
| UI | Swing |
| Look and Feel | FlatLaf 3.7.1 |
| JSON | Jackson Databind 2.17.2 |
| Código gerado | Lombok 1.18.38 |
| Nativos Windows | MinGW para build do file picker nativo |
| Nativos Linux | GTK3 dev e `pkg-config` para build do file picker nativo |
| Nativos macOS | toolchain com suporte a Objective-C++ |

---

## 3. Instalação como dependência

Artefato Maven do projeto:

```xml
<dependency>
    <groupId>dtm.stools</groupId>
    <artifactId>SwingTools</artifactId>
    <version>1.0.1</version>
</dependency>
```

Se o projeto ainda estiver apenas local, instale no repositório Maven local:

```bash
mvn clean install -Dnative.build.skip=true
```

Depois, use a dependência acima em outro projeto Maven.

---

## 4. Build local

Compilar sem testes:

```bash
mvn -DskipTests compile
```

Compilar sem rebuild dos binários nativos:

```bash
mvn -DskipTests -Dnative.build.skip=true compile
```

Gerar pacote:

```bash
mvn clean package -Dnative.build.skip=true
```

Gerar recursos nativos da plataforma atual:

```bash
mvn generate-resources
```

Executar os exemplos/classes de teste não usa uma suíte JUnit no momento. Os arquivos em `src/test/java/dtm/stools/examples` são demos executáveis com método `main`.

---

## 5. Estrutura do repositório

```text
SwingTools/
├── pom.xml
├── README.md
├── docs/
│   ├── JsonLookAndFeel_Documentacao.md
│   └── MaskedTextField_Documentacao.md
├── native/
│   ├── linux/
│   ├── mac/
│   └── win/
├── src/
│   ├── main/
│   │   ├── java/dtm/stools/
│   │   └── resources/
│   └── test/
│       └── java/dtm/stools/examples/
└── .github/workflows/build.yml
```

Principais diretórios:

| Caminho | Conteúdo |
|---|---|
| `src/main/java/dtm/stools/activity` | Activities e janelas de alto nível |
| `src/main/java/dtm/stools/activity/delegated` | Activities delegadas baseadas em controller |
| `src/main/java/dtm/stools/controllers` | Controllers de janela |
| `src/main/java/dtm/stools/controllers/component` | Controllers de componentes |
| `src/main/java/dtm/stools/context` | Estado, DOM, janelas, dialogs e notificações |
| `src/main/java/dtm/stools/component` | Componentes visuais e infra de componentes |
| `src/main/java/dtm/stools/component/inputfields` | Campos de formulário |
| `src/main/java/dtm/stools/component/menu` | Menu bar e popup menus |
| `src/main/java/dtm/stools/component/panels` | Painéis, tabs, dock, editor, gráficos, file picker e loading |
| `src/main/java/dtm/stools/component/tree` | Árvore avançada |
| `src/main/java/dtm/stools/component/grids` | Tabela baseada em modelo |
| `src/main/java/dtm/stools/configs` | Look and feel por JSON e system tray |
| `src/main/java/dtm/stools/layouts` | Layouts customizados |
| `src/main/java/dtm/stools/utils` | Utilitários de fonte, imagem e recursos |
| `src/main/resources/drawables` | Ícones internos |
| `src/main/resources/native` | Bibliotecas nativas empacotadas |
| `native` | Código-fonte e scripts para build dos nativos |
| `docs` | Documentação complementar de classes específicas |
| `src/test/java/dtm/stools/examples` | Demos manuais |

---

## 6. Arquitetura geral

A biblioteca é organizada em camadas:

1. **Janela e contexto:** `Activity`, `DialogActivity`, `IWindow`, `WindowContext`, `WindowExecutor`.
2. **Controller e binding:** classes abstratas de controller e anotações `@ViewRef` / `@ClientRef`.
3. **Componentes e eventos:** painéis base, componentes de formulário, eventos tipados e listeners.
4. **Componentes complexos:** menus, tabs, dock, tree, grid, file picker, code editor e painéis gráficos.
5. **Infra visual:** look and feel por JSON, FlatLaf, utilitários de fonte, imagem e recursos.
6. **Integração nativa:** file picker via bibliotecas `.dll`, `.dylib` e `.so`.

O Swing continua sendo a base. SwingTools não substitui `JFrame`, `JPanel`, `JTable`, `JTree` ou `JComponent`; ele cria wrappers, especializações e serviços auxiliares para trabalhar com eles de forma mais organizada.

---

## 7. Janelas, activities e ciclo de vida

Activities são janelas Swing com ciclo de vida padronizado.

| Classe | Base Swing | Uso |
|---|---|---|
| `Activity` | `JFrame` | Janela principal |
| `DialogActivity` | `JDialog` | Dialog modal ou não modal |
| `FragmentActivity` | `JDialog` | Fluxo secundário ou tela auxiliar |
| `TransientPopupActivity` | `JWindow` | Popup temporário |
| `NotificationActivity` | `JWindow` | Notificação visual empilhável |

Exemplo mínimo:

```java
import dtm.stools.activity.Activity;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.WindowEvent;

public class MinhaJanela extends Activity {

    @Override
    protected void onDrawing() {
        setTitle("Minha aplicação");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        JButton salvar = new JButton("Salvar");
        salvar.setName("salvar");
        add(salvar, BorderLayout.SOUTH);
    }

    @Override
    protected void onLoad(WindowEvent e) {
        JButton salvar = findById("salvar");
        salvar.addActionListener(event -> System.out.println("Salvando"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MinhaJanela().init());
    }
}
```

Pontos importantes:

| Método | Função |
|---|---|
| `init()` | Inicializa a janela, aplica configuração de tray, chama `onDrawing`, carrega DOM, registra eventos e mostra a janela |
| `requestClose()` | Solicita fechamento passando pelo fluxo de `onClose` |
| `dispose()` | Encerra recursos, remove tray icon e remove a janela do `WindowContext` |
| `findById(String)` | Busca o primeiro componente cujo `name` corresponde ao id |
| `findAllById(String)` | Busca todos os componentes com o mesmo id |
| `reloadDomElements()` | Recarrega a árvore interna de componentes |
| `putInClient(...)` | Salva estado associado à janela |
| `getFromClient(...)` | Recupera estado associado à janela |
| `runOnUi(...)` | Executa ação na EDT com a janela tipada |
| `getWindowExecutor()` | Acesso ao executor da janela |

Callbacks principais:

| Callback | Momento |
|---|---|
| `onDrawing()` | Montagem da UI |
| `onLoad(WindowEvent)` | Abertura da janela |
| `onClose(WindowEvent)` | Tentativa de fechamento |
| `onResize()` | Redimensionamento |
| `onMove()` | Movimento da janela |
| `onShow()` | Janela exibida |
| `onHidden()` | Janela ocultada |
| `onFocus(WindowEvent)` | Janela recebeu foco |
| `onLostFocus(WindowEvent)` | Janela perdeu foco |
| `onError(String, Throwable)` | Falha em ação interna do ciclo de vida |

`Activity` também possui integração com system tray. Ao fechar, se a configuração de tray estiver disponível, o comportamento padrão pergunta se a aplicação deve minimizar para a bandeja ou fechar.

---

## 8. Controllers e binding

Controllers separam comportamento da interface.

| Classe | Uso |
|---|---|
| `AbstractWindowController<T extends IWindow>` | Base genérica para controller de janela |
| `AbstractActivityWindowController` | Controller de `Activity` |
| `AbstractDialogActivityWindowController` | Controller de `DialogActivity` |
| `BindingAbstractWindowController<T>` | Controller com injeção por anotações |
| `AbstractViewController<T extends IWindowComponent>` | Controller de componente |
| `BindingAbstractViewController<T>` | Controller de componente com binding |

Anotações:

| Anotação | Origem do valor |
|---|---|
| `@ViewRef("id")` | Componente encontrado no DOM da janela pelo `setName("id")` |
| `@ClientRef("chave")` | Objeto salvo no estado client-side da janela |

Exemplo:

```java
import dtm.stools.activity.Activity;
import dtm.stools.context.annotations.ClientRef;
import dtm.stools.context.annotations.ViewRef;
import dtm.stools.controllers.BindingAbstractWindowController;

import javax.swing.JTextField;

public class PrincipalController extends BindingAbstractWindowController<Activity> {

    @ViewRef("nome")
    private JTextField nome;

    @ClientRef("usuario")
    private Usuario usuario;

    @Override
    public void onLoad(Activity window) {
        nome.setText(usuario.nome());
    }
}
```

Activities delegadas usam composição:

```java
import dtm.stools.activity.delegated.DelegatedActivity;

public class PrincipalActivity extends DelegatedActivity<PrincipalController> {
    @Override
    protected void onDrawing() {
        setSize(800, 500);
    }
}
```

---

## 9. Estado, DOM e navegação

### Estado client-side

Cada janela mantém um mapa de estado:

```java
putInClient("usuario", usuario);
putInClient("tema", "dark", true);

Usuario usuarioAtual = getFromClient("usuario");
String tema = getFromClient("tema", "light");
```

Esse estado é útil para dados de sessão da janela, objetos compartilhados com controller e valores temporários.

### DOM de componentes

SwingTools monta um índice dos componentes da janela usando `Component#setName`.

```java
JTextField campo = new JTextField();
campo.setName("nome");
add(campo);

JTextField nome = findById("nome");
```

Se componentes forem adicionados depois do carregamento inicial:

```java
reloadDomElements();
```

### WindowContext

`WindowContext` mantém uma pilha global de janelas `IWindow`.

Métodos úteis:

| Método | Uso |
|---|---|
| `pushWindow(window)` | Registra janela |
| `removeWindow(window)` | Remove janela |
| `getWindows()` | Itera janelas registradas |
| `peekWindow()` | Consulta topo da pilha |
| `peekLastWindow()` | Consulta última janela |
| `popWindow()` | Remove e retorna topo |
| `popUntilWindow(Class)` | Remove até encontrar tipo alvo |
| `reattachWindow(...)` | Reposiciona janela na pilha |
| `clear()` | Limpa contexto |

### WindowExecutor

`WindowExecutor` encapsula execução de ações com tratamento de erro. `Activity` usa esse executor internamente para callbacks de ciclo de vida.

---

## 10. Sistema de eventos

Componentes próprios usam `EventListenerComponent` e eventos derivados de `EventComponent`.

Padrão de uso:

```java
componente.addEventListner(EventType.CHANGE, event -> {
    Object valor = event.getValue();
});
```

Observação: o método público existente é `addEventListner`, com essa grafia.

Elementos principais:

| Classe/interface | Função |
|---|---|
| `EventType` | Constantes de eventos comuns |
| `EventComponent` | Contrato base do evento |
| `EventListenerComponent` | Contrato para componentes com listeners |
| `EventSubscription` | Representa inscrição de evento |
| `EventGridViewTable` | Evento de tabela |
| `EventMenuBar` / `MenuBarEvent` | Eventos de menu |
| `EventTabbedPanel` / `TabEvent` | Eventos de abas |
| `EventDockPanel` / `DockEvent` | Eventos de dock |
| `EventTreeView` / `EventTree` | Eventos de árvore |

---

## 11. Componentes base

| Classe | Descrição |
|---|---|
| `ViewPanel` | Painel base de visualização |
| `BlockingPanel` | Painel com recursos para bloquear interação/estado visual |
| `KeyPanel` | Painel orientado a chave/contexto |
| `LoadingPanel` | Painel de carregamento |
| `PanelEventListener` | Base para painéis com eventos |
| `DelegatedBlockingPanel` | `BlockingPanel` com controller delegado |
| `DelegatedKeyPanel` | `KeyPanel` com controller delegado |
| `WindowPanel` | Janela interna movel, redimensionavel e extensivel |
| `WindowDesktopPanel` | Host para janelas internas, modalidade, snap e layouts |
| `DelegatedWindowPanel` | `WindowPanel` com controller especializado |
| `DelegatedWindowDesktopPanel` | `WindowDesktopPanel` com controller especializado |
| `DelegatedIWindowComponent` | Base delegada para componentes de janela |
| `ComponentAnimator` | Utilitário de animação de componentes |

Esses componentes servem como base para os módulos maiores, como tabs, dock, file picker e inputs customizados.

---

## 12. Menus

### MenuBar

`MenuBar` cria uma barra de menu fluente, estilizada e compatível com FlatLaf.

Exemplo:

```java
MenuBar bar = new MenuBar(MenuBarStyle.dark());

MenuBar.Menu arquivo = bar.addMenu("file", "Arquivo");
arquivo.addItem("new", "Novo");
arquivo.addItem("open", "Abrir");
arquivo.addSeparatorLine();
arquivo.addItem("exit", "Sair", item -> item.addActionListener(e -> frame.dispose()));

frame.setJMenuBar(bar);
```

Recursos:

| Recurso | Descrição |
|---|---|
| `MenuBarStyle` | Tema visual da barra |
| `MenuBarConfig` | Configuração da barra |
| `MenuConfig` | Configuração de um menu |
| `MenuItemConfig` | Configuração de item |
| `GradientStop` | Suporte a gradientes |
| `MenuSchema` | Definição declarativa |
| `MenuNode` / `MenuTreeEditor` | Edição/representação em árvore |
| `MenuAction` | Ação de menu |
| `EventMenuBar` | Eventos tipados |

### CollapsibleMenuBar

`CollapsibleMenuBar` é uma variação voltada a menus recolhíveis.

### ActionPopupMenu

`ActionPopupMenu` e `ActionMenuSupport` ajudam a montar menus de contexto com estilo.

O pacote `component.menu.popup.style` contém:

| Classe | Uso |
|---|---|
| `ActionMenuStyle` | Define cores, fontes e espaçamentos |
| `BorderFactorySupplier` | Fábrica de bordas |

---

## 13. Abas e docking

### TabbedPanel

`TabbedPanel` encapsula `JTabbedPane` com recursos adicionais.

Exemplo:

```java
TabbedPanel tabs = new TabbedPanel();
String key = tabs.addTab("Editor.java", new CodeEditor("class Editor {}"));

tabs.setCloseButtonsVisible(true);
tabs.setCloseOnMiddleClickEnabled(true);
tabs.setRenameOnDoubleClickEnabled(true);
```

Quando as abas não cabem, o padrão é um menu de três pontos com as abas ocultas. Use `setTabOverflowMode(TabOverflowMode.SCROLL_BUTTONS)` para optar pelos chevrons de navegação.

Também é possível criar abas por `TabConfig`:

```java
tabs.addTab(new TabConfig("home", "Home", new JPanel())
        .closable(false)
        .pinned(true));
```

Recursos:

| Recurso | Descrição |
|---|---|
| Chaves por aba | Cada aba possui uma chave única |
| Fechamento | Fechar por botão, menu ou clique do meio |
| Evento antes de fechar | Cancelável |
| Pin | Aba fixada |
| Dirty state | Indicação de alteração |
| Badge | Marcador visual |
| MRU | Troca por histórico de uso |
| Drag | Reordenação |
| Split | Separação de abas por arraste |
| Header customizado | `TabHeaderFactory` |
| Menu customizado | `TabMenuProvider` |
| Estilo | `TabStyle` |

Eventos comuns:

| Evento | Momento |
|---|---|
| `TAB_ADD` | Aba adicionada |
| `BEFORE_TAB_CLOSE` | Antes de fechar; cancelável |
| `TAB_CLOSE` | Aba fechada |
| `BEFORE_TAB_REMOVE` | Antes de remover; cancelável |
| `TAB_REMOVE` | Aba removida |
| `CHANGE` | Seleção alterada |

### DockPanel

`DockPanel` organiza componentes em regiões.

Exemplo:

```java
DockPanel dock = new DockPanel();
dock.addDock("Projeto", new JScrollPane(tree), DockRegion.LEFT);
dock.addDock("Editor", new CodeEditor(), DockRegion.CENTER);
dock.addDock("Console", new JTextArea(), DockRegion.BOTTOM);
```

Por padrão, vários docks na mesma região são mostrados simultaneamente em divisores redimensionáveis. Por exemplo, `Build` e `Terminal` em `BOTTOM` ficam lado a lado. Use `setDockRegionLayout(DockRegionLayout.TABS)` para agrupá-los em abas ou `DockRegionLayout.SINGLE` para manter apenas um dock por região.

Regiões:

| Região | Uso típico |
|---|---|
| `CENTER` | Conteúdo principal |
| `LEFT` / `RIGHT` | Navegação, propriedades, ferramentas |
| `TOP` / `BOTTOM` | Painéis auxiliares |
| `TOP_LEFT`, `BOTTOM_LEFT`, `TOP_RIGHT`, `BOTTOM_RIGHT` | Composições laterais |

Recursos:

| Recurso | Descrição |
|---|---|
| `DockConfig` | Configuração declarativa de dock |
| `DockEntry` | Entrada registrada |
| `DockRegion` | Região alvo |
| `DockRegionLayout` | Exibição de docks na mesma região: `SPLIT`, `TABS` ou `SINGLE` |
| `DockDragPolicy` | Política de arraste |
| `DockDropContext` | Contexto de drop |
| `DockLayoutSnapshot` | Snapshot de layout |
| `DockGroupFactory` | Customização dos grupos |
| `DockSeparatorFactory` | Customização de separadores |

---

## 14. Campos de entrada

### Text fields

| Classe | Descrição |
|---|---|
| `JTextFieldListener` | `JTextField` com suporte a eventos |
| `MaskedTextField` | Campo com máscara, placeholder, readonly e texto limpo |
| `CurrencyField` | Campo monetário baseado em locale |
| `NumberField` | Campo numérico com locale, precisão, limites e passo |
| `SearchTextField<T>` | Campo com sugestões e busca |
| `PathTextField` | Campo visual para path dividido em segmentos |
| `PathSearchTextField<T>` | Combina path visual com busca |

Exemplo de máscara:

```java
MaskedTextField cpf = new MaskedTextField("###.###.###-##");
cpf.setPlaceholder("CPF");
cpf.setCleanText("12345678901");

String textoFormatado = cpf.getText();
String apenasDigitos = cpf.getCleanText();
```

Exemplo numérico:

```java
NumberField quantidade = new NumberField(Locale.forLanguageTag("pt-BR"))
        .setDecimalPlaces(2)
        .setRange(BigDecimal.ZERO, new BigDecimal("100"))
        .setStep(new BigDecimal("0.25"));

BigDecimal valor = quantidade.getValue();
```

Exemplo de busca:

```java
SearchTextField<Usuario> busca = new SearchTextField<>();
busca.setDataSource(usuarios);
busca.setDisplayFunction(Usuario::nome);
busca.addSearchOption(Usuario::email);
busca.setMinLength(2);
busca.setMaxResults(8);
```

### DropdownField

`DropdownField` encapsula `JComboBox` com data source, seleção e renderer.

```java
DropdownField status = new DropdownField("Ativo", "Inativo", "Pendente");
status.setPlaceholder("Selecione");
status.select("Ativo");
```

### SwitchField

`SwitchField` representa um toggle visual com animação, texto e cores customizáveis.

```java
SwitchField ativo = new SwitchField(true)
        .setShowText(true)
        .setTexts("Sim", "Não")
        .setSwitchSize(72, 32)
        .setThumbPadding(3);
```

### TagInputField

`TagInputField` gerencia múltiplas tags.

```java
TagInputField tags = new TagInputField();
tags.setAllowDuplicates(false);
tags.setMaxTags(5);
tags.addTag("java");
tags.addTag("swing");
```

Recursos:

| Recurso | Descrição |
|---|---|
| Validador | `setTagValidator(...)` |
| Normalizador | `setTagNormalizer(...)` |
| Separadores | `setSeparatorsRegex(...)` |
| Duplicidade | Controle case-sensitive ou não |
| Renderer | `TagRenderer` customizado |

### ColorPickerField

Campo de cor com preview e formato configurável.

```java
ColorPickerField color = new ColorPickerField(ColorFormat.HEX);
color.setColor(Color.BLUE);

String hex = color.getColorAsHex();
Color atual = color.getColor();
```

### DatePickerInputField

Campo de data com calendário embutido.

```java
DatePickerInputField data = new DatePickerInputField("dd/MM/yyyy");
data.setSelectedDate(LocalDate.now());

LocalDate selecionada = data.getSelectedDate();
String texto = data.getFormattedText();
```

---

## 15. File pickers

### OsFilePicker

`OsFilePicker` abre o seletor de arquivos nativo do sistema operacional.

```java
File file = OsFilePicker.openFile(
        "Abrir imagem",
        DeFilter.of("Imagens", "png", "jpg", "jpeg")
);

File[] files = OsFilePicker.openFiles("Selecionar arquivos");
File dir = OsFilePicker.openDirectory("Selecionar pasta");
File save = OsFilePicker.saveFile("Salvar", "arquivo.txt");
```

Métodos principais:

| Método | Retorno |
|---|---|
| `openFile(...)` | `File` |
| `openFiles(...)` | `File[]` |
| `saveFile(...)` | `File` |
| `openDirectory(...)` | `File` |
| `openFileOrDirectory(...)` | `File` |
| `openFilesOrDirectories(...)` | `File[]` |

Há overloads com `File initialDir`.

### FilePickerInputPanel

`FilePickerInputPanel` é um componente Swing de seleção de arquivos/pastas.

```java
FilePickerInputPanel picker = new FilePickerInputPanel();
picker.setMultiSelectionEnabled(true);
picker.setFileSelectionMode(FileSelectionMode.FILES_AND_DIRECTORIES);
picker.setShowHiddenFiles(false);
picker.setRequired(true);

picker.setOnEndSelection(panel -> {
    Set<File> selecionados = panel.getSelectedFiles();
});
```

Recursos:

| Recurso | Método |
|---|---|
| Seleção múltipla | `setMultiSelectionEnabled` |
| Modo de seleção | `setFileSelectionMode` |
| Filtro de extensão | `setFileFilter` |
| Arquivos ocultos | `setShowHiddenFiles` |
| Campo obrigatório | `setRequired` |
| Permitir novo arquivo | `setAllowNewFileInput` |
| Arquivo selecionado | `setSelectedFile`, `getSelectedFile`, `getSelectedFiles` |

---

## 16. GridViewTable

`GridViewTable<T>` é uma tabela baseada em modelo Java e reflexão.

Exemplo:

```java
public class Usuario {
    @GridColumn(name = "Nome", order = 1)
    private String nome;

    @GridColumn(name = "Ativo", order = 2)
    private boolean ativo;
}

GridViewTable<Usuario> table = new GridViewTable<>(Usuario.class);
table.setDataSource(usuarios);
table.setPaginationEnabled(true);
table.setPageSize(20);
```

Recursos:

| Recurso | Descrição |
|---|---|
| `@GridColumn` | Define coluna via anotação |
| `ReflectionTableModel<T>` | Modelo gerado por reflexão |
| `TableGridMode.SINGLE` | Seleção única |
| `TableGridMode.BATCH` | Seleção múltipla |
| `allowEdit` | Controla edição |
| Paginação | `setPaginationEnabled`, `setPageSize`, `goToPage` |
| Ordenação | `setAutoCreateRowSorter(true)` é habilitado no construtor |
| Boolean | Usa renderer/editor boolean padrão |
| Coleções/arrays | Podem usar `DropdownField` como editor |

Métodos úteis:

| Método | Uso |
|---|---|
| `setDataSource(Collection<T>)` | Define dados |
| `getTotalItems()` | Total de itens |
| `getTotalPages()` | Total de páginas |
| `goToPage(int)` | Navega para página |
| `setPageSizeOptions(List<Integer>)` | Opções de tamanho |
| `setGridMode(TableGridMode)` | Modo de seleção |
| `setAllowEdit(boolean)` | Edição de células |

---

## 17. TreeView

`TreeView<T>` é uma árvore avançada baseada em `JTree` e `TreeNode<T>`.

Exemplo:

```java
TreeNode<String> root = new TreeNode<>("root", "Projeto");
TreeNode<String> src = new TreeNode<>("src", "src");
TreeNode<String> pom = new TreeNode<>("pom", "pom.xml");
src.setCheckable(true);
pom.setCheckable(true);
root.add(src);
root.add(pom);

TreeView<String> tree = new TreeView<>(root);
tree.setMode(TreeViewMode.SINGLE);
tree.setToggleCheckOnRowClick(true);
```

Recursos:

| Recurso | Descrição |
|---|---|
| Modos | Seleção simples, múltipla e descontínua |
| Checkbox | Nós marcáveis por `TreeNode#setCheckable`, com propagação para filhos e pais |
| Busca | Termo de busca e case-sensitive opcional |
| Filtro | `TreeFilter<T>` |
| Lazy load | `TreeNodeProvider` |
| Edição | `TreeNodeEditor` |
| Render | `TreeNodeRenderer` |
| Popup | `TreePopupContext<T>` |
| Drag and drop | Interno e externo |
| Políticas de drop | `TreeDropContext<T>` e `TreeExternalDropContext<T>` |
| Teclado | Enter, Delete, F2, Space e setas |
| Highlight | Animação de destino de drop |

Configurações relevantes:

| Propriedade | Uso |
|---|---|
| `propagateCheckDown` | Propaga check para filhos |
| `propagateCheckUp` | Atualiza check dos pais |
| `toggleCheckOnRowClick` | Alterna checkbox clicando na linha |
| `expandOnDoubleClick` | Expande no duplo clique |
| `editOnF2` | Edita com F2 |
| `removeSelectedOnDelete` | Remove com Delete |
| `activateOnEnter` | Ativa com Enter |
| `toggleCheckOnSpace` | Alterna checkbox com Space |
| `dragAndDropEnabled` | Habilita drag/drop interno |
| `externalDropEnabled` | Habilita drop externo |

---

## 18. CodeEditor

`CodeEditor` é um componente de editor de código construído sobre `BlockingPanel`.

Exemplo:

```java
CodeEditor editor = new CodeEditor("""
        public class App {
            public static void main(String[] args) {
                System.out.println("Hello");
            }
        }
        """);

editor.setHighlightCurrentLine(true);
editor.setFoldingEnabled(true);
editor.addFoldRule(FoldRule.pair('{', '}'));
editor.addBreakpoint(2);
```

Componentes internos:

| Classe | Função |
|---|---|
| `CodeEditor` | Componente principal |
| `CodeEditorTextArea` | Área de texto customizada |
| `CodeEditorScrollPane` | Scroll com gutter/minimap |
| `CodeEditorGutter` | Gutter lateral |
| `CodeEditorMinimap` | Preview/minimap |
| `TextBuffer` | Buffer de texto |
| `SearchPanel` / `SearchEngine` | Busca e substituição |

Camadas de gutter:

| Camada | Função |
|---|---|
| `LineNumberLayer` | Numeração de linhas |
| `BreakpointLayer` | Breakpoints |
| `BookmarkLayer` | Bookmarks |
| `FoldingLayer` | Folding |
| `LineMarkerLayer` | Marcadores de linha |
| `GutterLayer` | Base para camadas customizadas |

Recursos:

| Recurso | Descrição |
|---|---|
| Busca | Painel de busca top/bottom/popup |
| Breakpoints | Marcação no gutter e atalho F9 |
| Bookmarks | Marcação de linhas |
| Folding | Regras por par, XML tags e regras customizadas |
| Minimap | Modo de visibilidade configurável |
| Highlight | Linha atual e intervalos estilizados |
| Providers | Extensão por interfaces |
| Hover | Tooltip/documentação por palavra |
| Context menu | Menu customizado por provider |
| Code actions | Ações e comandos |
| Diagnostics | Erros, avisos e informações |
| Autocomplete | Sugestões e snippets |
| CodeLens | Itens acima/ao lado do código |
| Inlay hints | Dicas inline |
| Rename/references/definition | APIs para navegação semântica |
| Formatting | Formatter por provider |

Providers disponíveis:

| Provider | Uso |
|---|---|
| `TokenizerCodeEditorProvider` | Tokenização |
| `TokenClassifierCodeEditorProvider` | Classificação de tokens |
| `TokenColorProvider` | Cores por token |
| `TokenRenderCodeEditorProvider` | Render customizado |
| `BracketMatcher` | Match de brackets |
| `WordDetector` | Detecção de palavra |
| `ContextMenuProvider` | Menu de contexto |
| `DefinitionProvider` | Go to definition |
| `ReferencesProvider` | Referências |
| `RenameProvider` | Rename |
| `DocumentSymbolProvider` | Símbolos do documento |
| `SelectionRangeProvider` | Expansão de seleção |
| `CodeActionProvider` | Ações de código |
| `AutoCompleteProvider` | Autocomplete |
| `DiagnosticsProvider` | Diagnósticos |
| `HoverDocumentationProvider` | Documentação no hover |
| `InlayHintProvider` | Inlay hints |
| `CodeLensProvider` | CodeLens |
| `CodeFormatter` | Formatação |
| `CommentProvider` | Comentários |

`HoverDocumentationProvider` pode retornar `new HoverInfo(texto)`, `HoverInfo.html(html)` ou `HoverInfo.markdown(markdown)`.

Listeners disponíveis:

| Listener | Evento |
|---|---|
| `DocumentEditListener` | Texto alterado |
| `LineChangeListener` | Linha atual alterada |
| `BreakpointChangeListener` | Breakpoint adicionado/removido/ativado/inativado (recebe `Breakpoint`, `added`) |
| `BookmarkChangeListener` | Bookmark adicionado/removido |
| `CodeEditorStateListener` | Estado do editor |
| `HoverListener` | Hover |
| `SearchRequestListener` | Solicitação de busca |

---

## 19. GraphicsPanel e GraphicsGlPanel

Esta secao resume o pacote `dtm.stools.component.panels.graphics`. A documentacao detalhada fica em `docs/Graphics.md`, com paginas especificas para `AbstractGraphicsPanel` e `GraphicsGlPanel`.

A base gráfica fica em `dtm.stools.component.panels.graphics`. O contrato principal é `AbstractGraphicsPanel<C extends GraphicsContext>`, que padroniza renderer, loop de render, FPS, VSync, input e ciclo de vida.

`GraphicsGlPanel` é a implementação OpenGL em `dtm.stools.component.panels.graphics.gl`. Por padrão, apresenta os frames por buffer em um componente Swing leve; use `GraphicsGlPresentationMode.HEAVYWEIGHT` no construtor quando precisar da superfície AWT direta.

```java
import dtm.stools.component.panels.graphics.gl.GL;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;
import dtm.stools.component.panels.graphics.gl.GraphicsGlPanel;
import dtm.stools.component.panels.graphics.gl.GraphicsGlRender;

GraphicsGlPanel panel = new GraphicsGlPanel(new GraphicsGlRender() {
    @Override
    public void render(GraphicsGlContext context) {
        GL.glClearColor(0.1f, 0.1f, 0.14f, 1f);
        GL.glClear(GL.GL_COLOR_BUFFER_BIT);
    }
});
panel.setFPS(60);
panel.setVsync(true);
```

Principais contratos:

Arquitetura pratica:

- O painel Swing hospeda uma superficie nativa (`Canvas`) e nao desenha por `paintComponent`.
- O renderer roda na thread de render GL; componentes Swing continuam na EDT.
- `setRenderer(...)` define ou troca o renderer; `getRenderer()` retorna a instancia atualmente configurada.
- Recursos GL devem ser criados em `initialize(...)`, desenhados/atualizados em `render(...)`, ajustados em `resize(...)` e liberados em `dispose(...)`.
- Multithread pode preparar dados, mas a acao de desenho deve ficar no renderer.
- `runOnUiThread(...)` neste modulo significa thread do contexto grafico, nao EDT do Swing; use como recurso de escape para tarefa curta de contexto, nao como fluxo normal de desenho.

| Classe/interface | Uso |
|---|---|
| `AbstractGraphicsPanel<C>` | Base para painéis gráficos |
| `GraphicsRender<C>` | Callbacks `initialize`, `render`, `resize` e `dispose` |
| `GraphicsContext` | Tamanho, input e `runOnUiThread` |
| `GraphicsInput` | Estado de teclado, mouse e scroll |
| `GraphicsInputState` | Implementacao ligada a listeners AWT/Swing |
| `GraphicsHost<C>` | Ponte entre painel e backend grafico concreto |
| `RenderThreadingMode` | Scheduler `SHARED` ou `INDIVIDUAL` |
| `GraphicsGlPanel` | Painel OpenGL concreto |
| `GraphicsGlRender` | Renderer OpenGL com helper `runOnUiThread` |
| `GraphicsGlContext` | Contexto GL com FPS, delta time e frame count |
| `GL` | Bindings nativos OpenGL expostos em Java |

Ciclo de vida resumido:

1. Crie o `GraphicsGlPanel` e defina o renderer.
2. Configure `setFPS`, `setVsync` e `setRenderMode` antes de mostrar o painel.
3. Ao entrar na hierarquia Swing, o painel registra o host no scheduler.
4. A thread de render cria o contexto nativo quando o canvas esta displayable.
5. O renderer recebe `initialize(...)`, depois `resize(...)` e entao `render(...)` a cada frame.
6. Ao trocar renderer ou descartar o painel, o renderer anterior recebe `dispose(...)`.

`getRenderer()` e util para recuperar a instancia configurada no painel, mas nao garante que ela ja tenha sido inicializada no contexto GL.

Threading:

| Operacao | Thread correta |
|---|---|
| Criar/adicionar componentes Swing | EDT |
| Manipular `JButton`, `JFrame`, labels e layouts | EDT |
| Chamar `GL.*`, criar buffers, shaders, VAOs e uniforms | Thread de render GL |
| Atualizar/desenhar recursos GL | Preferencialmente dentro do renderer; `panel.runOnUiThread(...)` ou `renderer.runOnUiThread(...)` so como recurso de escape para tarefa curta de contexto |

Modos de render:

| Modo | Uso |
|---|---|
| `RenderThreadingMode.SHARED` | Padrao. Varios paineis dividem uma thread `SwingTools-GL-Render`. |
| `RenderThreadingMode.INDIVIDUAL` | Cada painel usa uma thread propria. Indicado para cenas pesadas ou sensiveis a latencia. |

Input por frame:

| Metodo | Uso |
|---|---|
| `isKeyDown(KeyEvent.VK_...)` | Consulta tecla pressionada |
| `isMouseButtonDown(MouseEvent.BUTTON...)` | Consulta botao do mouse |
| `getMouseX()` / `getMouseY()` | Posicao mais recente do mouse na superficie |
| `isMouseInside()` | Indica se o mouse esta dentro do canvas |
| `getWheelRotation()` | Rolagem acumulada; guarde o valor anterior se precisar de delta por frame |

Cuidados principais:

- Configure `setFPS`, `setVsync` e `setRenderMode` antes de mostrar o painel quando possível.
- Prefira tocar em recursos GL dentro do renderer. Workers podem preparar dados, mas o desenho deve acontecer em `render(...)`. Use `runOnUiThread(...)` apenas como recurso de escape quando algum codigo externo saiu do renderer e realmente precisar executar uma tarefa curta no contexto GL.
- Chame `dispose()` ao fechar janelas descartáveis para liberar o contexto e os recursos nativos.
- Chame `requestFocusInWindow()` se o renderer depender de teclado.
- `GraphicsGlPanel` depende do nativo `graphicsgl`; Windows usa `graphicsgl.dll`, Linux usa `libgraphicsgl.so` quando empacotado, e macOS ainda nao e suportado pelo loader GL atual.
- Veja detalhes em `docs/Graphics.md`, `docs/AbstractGraphicsPanel.md` e `docs/GraphicsGlPanel.md`.

---

## 20. Dialogs, popups e notificações

### Dialogs

`Dialogs` é uma fachada para `ModernDialog` e `ModernInputDialog`.

```java
Dialogs.info("Operação concluída");
Dialogs.error("Falha ao salvar");

boolean ok = Dialogs.confirm("Deseja continuar?");
String nome = Dialogs.input("Informe o nome");
```

Builder:

```java
int result = Dialogs.modernDialogBuilder()
        .title("Remover")
        .message("Remover item selecionado?")
        .type(ModernDialog.Type.QUESTION)
        .option("Cancelar", 0)
        .option("Remover", 1, Color.RED, Color.WHITE)
        .show();
```

Input com validação:

```java
String value = ModernInputDialog.modernDialogBuilder()
        .title("Nome")
        .message("Informe o nome")
        .input(new JTextField())
        .onValidate(ctx -> !ctx.value().isBlank())
        .disableConfirmWhenInvalid(true)
        .show();
```

Dialog com componente customizado e retorno tipado:

```java
Usuario usuario = Dialogs.componentBuilder(Usuario.class)
        .title("Usuario")
        .form(form -> form.field("nome", "Nome", new JTextField()))
        .result(ctx -> new Usuario(ctx.form().text("nome")))
        .show();
```

### PopupBuilder

Cria `JDialog` leve com conteúdo customizado:

```java
PopupBuilder.create()
        .title("Detalhes")
        .size(400, 260)
        .content(new JPanel())
        .modal(true)
        .showAndWait();
```

### Notifications

`Notifications` cria toasts modernos.

```java
Notifications.showInfo("Sincronizado");

Notifications.modernDialogBuilder()
        .type(Notifications.Type.SUCCESS)
        .title("Salvo")
        .message("Registro salvo com sucesso")
        .duration(3, TimeUnit.SECONDS)
        .action("Abrir", () -> System.out.println("Abrir"))
        .show();
```

`NotificationManager` controla a fila de `NotificationActivity`, tempo de exibição, espaçamento e shutdown.

---

## 21. FlexBoxLayout

`FlexBoxLayout` implementa `LayoutManager2` inspirado em CSS Flexbox.

Exemplo:

```java
JPanel panel = new JPanel(FlexBoxLayout.modernDialogBuilder()
        .direction(FlexBoxLayout.Direction.ROW)
        .justify(FlexBoxLayout.Justify.SPACE_BETWEEN)
        .align(FlexBoxLayout.Align.CENTER)
        .gap(12)
        .padding(16)
        .build());

panel.add(new JButton("A"), FlexBoxLayout.FlexConstraints.of().grow(1));
panel.add(new JButton("B"), FlexBoxLayout.FlexConstraints.of().fixedWidth(120));
```

Configurações:

| Opção | Uso |
|---|---|
| `direction` | `ROW` ou `COLUMN` |
| `justify` | `START`, `CENTER`, `END`, `SPACE_BETWEEN`, `SPACE_AROUND`, `SPACE_EVENLY` |
| `align` | `START`, `CENTER`, `END`, `STRETCH` |
| `wrap` | Quebra linha/coluna |
| `reverse` | Inverte ordem |
| `gap`, `hgap`, `vgap` | Espaçamento |
| `padding` | Espaçamento interno |

Constraints:

| Constraint | Uso |
|---|---|
| `grow` | Crescimento relativo |
| `basis` | Tamanho base |
| `widthPercent` / `heightPercent` | Percentual |
| `fixedWidth` / `fixedHeight` | Tamanho fixo |
| `minWidth` / `maxWidth` | Limites de largura |
| `minHeight` / `maxHeight` | Limites de altura |
| `alignSelf` | Alinhamento individual |

Também existe:

```java
JPanel scrollable = FlexBoxLayout.scrollablePanel(modernDialogBuilder -> modernDialogBuilder
        .direction(FlexBoxLayout.Direction.COLUMN)
        .gap(8)
        .padding(12));
```

---

## 22. JsonLookAndFeel

`JsonLookAndFeel` aplica tema visual em Swing a partir de JSON, usando Jackson e `UIManager`.

Uso rápido:

```java
JsonLookAndFeel.apply(Path.of("theme.json"));
```

Também aceita:

```java
JsonLookAndFeel.apply(jsonString);
JsonLookAndFeel.apply(file);
JsonLookAndFeel.apply(inputStream);
JsonLookAndFeel.apply(themeMap);
JsonLookAndFeel.apply(jsonString, false);
```

Atualização manual:

```java
JsonLookAndFeel.updateOpenWindows();
JsonLookAndFeel.applyDefaultsToTree(panel);
SwingUtilities.updateComponentTreeUI(panel);
```

Exemplo reduzido de tema:

```json
{
  "lookAndFeel": "com.formdev.flatlaf.FlatDarkLaf",
  "font": {
    "family": "Segoe UI",
    "size": 13,
    "style": "plain"
  },
  "colors": {
    "background": "#1E1E1E",
    "foreground": "#F5F5F5",
    "primary": "#3B82F6",
    "border": "#3A3A3A"
  },
  "components": {
    "Button": {
      "background": "#3B82F6",
      "foreground": "#FFFFFF",
      "padding": [8, 14],
      "borderRadius": 8
    },
    "TextField": {
      "background": "#252526",
      "foreground": "#FFFFFF",
      "caret": "#FFFFFF"
    }
  }
}
```

A documentação mais detalhada fica em `docs/JsonLookAndFeel_Documentacao.md`.

---

## 23. System tray

`Activity` possui suporte a system tray via `SystemTrayConfiguration`.

Pontos relevantes:

| Item | Descrição |
|---|---|
| `applySystemTrayConfiguration(...)` | Hook para configurar tray |
| `isSystemTrayEnable()` | Verifica disponibilidade |
| `windowInTray()` | Verifica se a janela está na bandeja |
| `onSystemTrayClick(...)` | Hook de eventos do ícone |
| `restoreFromTray()` | Restaura janela |
| `minimizeToTray()` | Minimiza para bandeja |
| `exitApplication()` | Remove ícone e encerra aplicação |

Por padrão, clique esquerdo no ícone restaura a janela.

---

## 24. Utilitários

### FontUtils

| Método | Uso |
|---|---|
| `hasGlobalFont(...)` | Verifica fonte global |
| `setGlobalFont(Font)` | Define fonte global no UIManager |
| `isFontInstalled(String)` | Verifica fonte instalada |
| `listInstalledFonts()` | Lista fontes instaladas |

### ImageUtils

| Método | Uso |
|---|---|
| `getImageIconByResource(...)` | Carrega `ImageIcon` |
| `getImageByResource(...)` | Carrega `Image` |
| `getColoredImageIconByResource(...)` | Carrega e recolore ícone |
| `changeImageColor(...)` | Troca cor da imagem |
| `changeIconColor(...)` | Troca cor de `ImageIcon` |
| `invertImageColors(...)` | Inverte cores |
| `resizeImageIcon(...)` | Redimensiona ícone |
| `textToIcon(...)` | Converte texto em ícone |

### ResourceUtils

| Método | Uso |
|---|---|
| `getResource(...)` | Resolve URL de recurso |
| `getResourceAsStream(...)` | Abre stream |
| `getExternalResourceAsStream(...)` | Recurso externo |
| `getResourceBytesExternal(...)` | Bytes de recurso externo |
| `getResourceClassPath(...)` | Path da classe/recurso |

---

## 25. Recursos nativos

O projeto contém um file picker nativo por plataforma e suporte nativo para o contexto OpenGL usado pelo `GraphicsGlPanel`.

| Plataforma | Fonte | Binário empacotado |
|---|---|---|
| Windows | `native/win/OsFilePicker.cpp` | `src/main/resources/native/win/amd64/osfilepicker.dll` |
| Windows | `native/win/GraphicsGl.cpp` | `src/main/resources/native/win/amd64/graphicsgl.dll` |
| macOS | `native/mac/OsFilePicker.mm` | `src/main/resources/native/mac/aarch64/libosfilepicker.dylib` |
| Linux | `native/linux/OsFilePicker.cpp` | `src/main/resources/native/linux/amd64/libosfilepicker.so` |
| Linux | `native/linux/GraphicsGl.cpp` | fonte disponivel em `native/linux`; empacotamento depende do build nativo |

Scripts:

| Script | Uso |
|---|---|
| `native/win/build.bat` | Build Windows |
| `native/win/build-mingw.bat` | Build Windows com MinGW |
| `native/win/build-gl-mingw.bat` | Build do suporte OpenGL no Windows |
| `native/mac/build.sh` | Build macOS |
| `native/linux/build.sh` | Build Linux |
| `native/linux/build-gl.sh` | Build do suporte OpenGL no Linux |

O `pom.xml` possui perfis ativados por sistema operacional:

| Perfil | Ativação |
|---|---|
| `native-windows` | Windows |
| `native-mac` | macOS |
| `native-linux` | Linux |

Para pular o build nativo:

```bash
mvn package -Dnative.build.skip=true
```

---

## 26. Exemplos disponíveis

Os exemplos ficam em `src/test/java/dtm/stools/examples`:

| Exemplo | Demonstra |
|---|---|
| `ActionPopupMenuExample` | Menu de contexto estilizado |
| `CodeEditorContextMenuExample` | Provider de menu de contexto no editor |
| `CodeEditorMarkerEventsExample` | Breakpoints, bookmarks e eventos de marker |
| `CodeEditorTabsExample` | `CodeEditor` dentro de `TabbedPanel` |
| `GraphicsGlPanelExample` | Triângulo OpenGL, input, VSync e FPS |
| `GraphicsGlCubeExample` | Cubo 3D usando `GraphicsGlPanel` |
| `GraphicsGlParallelRunOnUiExample` | Geração paralela com uso excepcional de `runOnUiThread` para atualizar recurso GL |
| `MenuBarFlatLafThemeExample` | `MenuBar` com FlatLaf dark/light |
| `ModernComponentDialogExample` | Dialog moderno com componente customizado e retorno tipado |
| `ModernInputDialogExample` | Input dialog moderno |
| `TabbedPanelDockModeExample` | Abas em modo dock/split |
| `TabbedPanelExample` | Operações básicas de abas |
| `TabbedPanelNestedInTabbedPaneExample` | `TabbedPanel` dentro de `JTabbedPane` |
| `TabbedPanelOverflowExample` | Overflow moderno com muitas abas em uma linha |
| `TreeViewPredicateUpdateExample` | Atualização/filtro de árvore |

Como são demos Swing, a forma mais simples é executar a classe desejada pela IDE.

---

## 27. CI e empacotamento

O workflow `.github/workflows/build.yml` faz build multi-plataforma:

1. Executa em Windows, macOS e Linux.
2. Configura JDK 21.
3. Instala dependências nativas necessárias.
4. Roda `mvn -B generate-resources` para gerar os binários nativos.
5. Envia os nativos como artifacts.
6. Em Ubuntu, baixa os artifacts das três plataformas.
7. Mescla tudo em `src/main/resources/native`.
8. Gera o JAR com `mvn -B clean package -Dnative.build.skip=true`.
9. Publica artifact `SwingTools-fat-jar`.
10. Em push na `main`, commita os binários nativos atualizados.
11. Em tags `v*`, anexa JAR e nativos à release.

O `maven-jar-plugin` exclui:

| Exclude | Motivo |
|---|---|
| `dtm/stools/Main.class` | Classe de entrada local |
| `dtm/stools/examples/**` | Exemplos não entram no artefato principal |

---

## 28. Status e observações

Este projeto é uma biblioteca Swing em evolução com foco em uso prático. Há muitos componentes funcionais, exemplos manuais e documentação complementar para partes específicas.

Pontos a observar:

| Ponto | Observação |
|---|---|
| Testes automatizados | O diretório `src/test` contém exemplos executáveis, não uma suíte de testes unitários estruturada |
| APIs públicas | Algumas APIs preservam nomes existentes, como `addEventListner` |
| Nativos | Builds nativos dependem do sistema operacional e toolchain local |
| Swing/EDT | Como em qualquer aplicação Swing, atualizações visuais devem respeitar a Event Dispatch Thread |
| Lombok | IDE e build precisam estar configurados para annotation processing |
| Documentação extra | `docs/JsonLookAndFeel_Documentacao.md` e `docs/MaskedTextField_Documentacao.md` detalham partes específicas |

---

## Exemplo completo

```java
import dtm.stools.activity.Activity;
import dtm.stools.component.grids.GridViewTable;
import dtm.stools.component.grids.annotations.GridColumn;
import dtm.stools.component.inputfields.textfield.SearchTextField;
import dtm.stools.component.panels.dock.DockPanel;
import dtm.stools.component.panels.dock.DockRegion;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRule;
import dtm.stools.component.panels.tab.TabConfig;
import dtm.stools.component.panels.tab.TabbedPanel;
import dtm.stools.component.tree.TreeNode;
import dtm.stools.component.tree.TreeView;
import dtm.stools.context.Notifications;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.util.List;

public class DemoSwingTools extends Activity {

    @Override
    protected void onDrawing() {
        setTitle("SwingTools demo");
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        DockPanel dock = new DockPanel();
        dock.addDock("Projeto", new JScrollPane(createTree()), DockRegion.LEFT);
        dock.addDock("Editor", createTabs(), DockRegion.CENTER);
        dock.addDock("Usuários", new JScrollPane(createTable()), DockRegion.BOTTOM);

        add(dock, BorderLayout.CENTER);
    }

    @Override
    protected void onLoad(WindowEvent e) {
        Notifications.showInfo("Aplicação carregada");
    }

    private TreeView<String> createTree() {
        TreeNode<String> root = new TreeNode<>("root", "Projeto");
        root.add(new TreeNode<>("src", "src"));
        root.add(new TreeNode<>("pom", "pom.xml"));
        return new TreeView<>(root);
    }

    private TabbedPanel createTabs() {
        CodeEditor editor = new CodeEditor("""
                public class App {
                    public static void main(String[] args) {
                        System.out.println("SwingTools");
                    }
                }
                """);
        editor.setHighlightCurrentLine(true);
        editor.setFoldingEnabled(true);
        editor.addFoldRule(FoldRule.pair('{', '}'));

        SearchTextField<String> search = new SearchTextField<>();
        search.setDataSource(List.of("Activity", "DockPanel", "CodeEditor", "TreeView"));
        search.setDisplayFunction(value -> value);

        TabbedPanel tabs = new TabbedPanel();
        tabs.addTab(new TabConfig("editor", "App.java", editor));
        tabs.addTab(new TabConfig("search", "Busca", search));
        return tabs;
    }

    private GridViewTable<Usuario> createTable() {
        GridViewTable<Usuario> table = new GridViewTable<>(Usuario.class);
        table.setDataSource(List.of(
                new Usuario("Ana", true),
                new Usuario("Bruno", false)
        ));
        table.setPaginationEnabled(true);
        table.setPageSize(10);
        return table;
    }

    public static class Usuario {
        @GridColumn(name = "Nome", order = 1)
        private final String nome;

        @GridColumn(name = "Ativo", order = 2)
        private final boolean ativo;

        public Usuario(String nome, boolean ativo) {
            this.nome = nome;
            this.ativo = ativo;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DemoSwingTools().init());
    }
}
```
