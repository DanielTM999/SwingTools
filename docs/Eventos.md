# Eventos

O sistema de eventos do SwingTools e uma camada leve sobre callbacks Java. Ele nao substitui os listeners nativos do Swing; ele padroniza eventos de componentes da biblioteca.

## Contrato base

Componentes com eventos implementam `EventListenerComponent`:

```java
void addEventListner(String eventType, Consumer<EventComponent> event);
void removeEventListner(String eventType, Consumer<EventComponent> event);
void removeEventListner(String eventType);
void removeAllListeners();
Map<String, List<Consumer<EventComponent>>> getEventListners();
```

Observacao: a API publica usa a grafia `Listner` porque o metodo foi publicado assim no codigo.

## Heranca comum

```text
EventListenerComponent
  PanelEventListener
    KeyPanel
    TabbedPanel
    DockPanel
    SwitchField

EventListenerComponent
  DataTableListener extends JTable
    GridViewTable<T>

EventListenerComponent
  JTextFieldListener extends JTextField
    MaskedTextField
      CurrencyField
    SearchTextField<T>

EventListenerComponent
  DropdownFieldListener<T> extends JComboBox<T>
    DropdownField
```

## Payload

`EventComponent` carrega:

| Metodo | Uso |
|---|---|
| `getComponent()` | Componente que emitiu o evento |
| `getValue()` | Valor bruto |
| `tryGetValue()` | Conversao por generics; retorna `null` se falhar |
| `getEventType()` | Nome do evento |
| `getProperties()` | Mapa com metadados extras |

Exemplo:

```java
field.addEventListner(EventType.CHANGE, event -> {
    String value = event.tryGetValue();
    System.out.println("Novo valor: " + value);
});
```

## Eventos comuns

`EventType` define:

| Constante | Valor | Uso comum |
|---|---|---|
| `CHANGE` | `change` | Valor ou estado mudou |
| `BEFORE_CHANGE` | `beforeChange` | Mudanca ainda pode ser cancelada pelo payload |
| `SELECT` | `select` | Item selecionado |
| `INPUT` | `input` | Digitacao/alteracao incremental |
| `LOAD` | `load` | Componente carregado |
| `CLEAR` | `clear` | Conteudo limpo |
| `RESIZE` | `resize` | Componente recalculou tamanho |
| `SUBMIT` | `submit` | Enter ou confirmacao |

## Cancelamento

Alguns componentes colocam um payload cancelavel em `BEFORE_*`. Exemplo em `KeyPanel`:

```java
keyPanel.addEventListner(EventType.BEFORE_CHANGE, event -> {
    KeyPanelContextChangeEvent change = event.tryGetValue();
    if (!canLeaveCurrentPanel()) {
        change.cancel();
    }
});
```

Nem todo evento `BEFORE_*` e cancelavel; confira a doc do componente.

## Eventos especializados

| Componente | Tipo/constantes | Exemplo |
|---|---|---|
| `GridViewTable` | `EventGridViewTable` | `SELECTION_ROW`, `SELECTION_COLUMN`, `CELL_EDIT` |
| `TabbedPanel` | `EventTabbedPanel` | add, close, remove, move, split, drag, dirty, badge |
| `DockPanel` | `EventDockPanel` | add, close, remove, select, move |
| `WindowPanel` / `WindowDesktopPanel` | `EventWindowPanel`, `WindowEvent`, `WindowAnimationEvent` | abertura, fechamento, estado, drag, resize, propriedades, snap, preview e assistente de Snap Layouts, animacao, modalidade, barra minimizada, menu contextual e layout |
| `TreeView` | `EventTreeView` / `EventTree` | selecao, check, expand, edit, drop |
| `SwitchField` | `SwitchField.SWITCH_ON`, `SwitchField.SWITCH_OFF` | liga/desliga |

## Remocao de listeners

Guarde a mesma instancia de callback se precisar remover depois:

```java
Consumer<EventComponent> listener = event -> System.out.println(event.getValue());

component.addEventListner(EventType.CHANGE, listener);
component.removeEventListner(EventType.CHANGE, listener);
```

Use `removeEventListner(type)` para limpar todos os listeners de um tipo e `removeAllListeners()` para limpar tudo.
