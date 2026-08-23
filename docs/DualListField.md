# DualListField

`DualListField<T>` e um campo de transferencia: duas listas lado a lado e uma coluna de botoes no meio que move os itens entre "disponiveis" e "selecionados".

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.duallistfield` |
| Heranca | `DualListField<T> extends PanelEventListener` |
| Uso principal | Escolher um subconjunto de itens, com ordem opcional |

## Estrutura

```text
[ titulo + filtro ]   [  >  ]   [ titulo + filtro ]
[ lista disponiveis]  [  >> ]   [ lista selecionados ]
                      [  <  ]
                      [  << ]
                      [  ^  ]   (somente com setReorderable(true))
                      [  v  ]
```

O layout usa [FlexBoxLayout](../src/main/java/dtm/stools/layouts/FlexBoxLayout.java) em `ROW` com `align STRETCH`; as listas crescem e a coluna central tem largura fixa.

## Uso basico

```java
DualListField<String> perfis = new DualListField<>(List.of(
        "Administrador", "Financeiro", "Suporte", "Vendas"));

perfis.setTitles("Perfis disponiveis", "Perfis do usuario")
      .setReorderable(true)
      .setShowFilter(true)
      .setMaxSelected(3);

perfis.addEventListener(EventType.CHANGE, event -> {
    List<String> selecionados = event.tryGetValue();
});
```

## Dados

| Metodo | Contrato |
|---|---|
| `setAvailable(List<T>)` | Substitui os disponiveis; remove os que ja estao selecionados |
| `setSelected(List<T>)` | Substitui os selecionados e dispara evento |
| `setSelected(List<T>, boolean fireEvent)` | Mesma coisa controlando o disparo |
| `getAvailable()` / `getSelected()` | Copias imutaveis na ordem corrente |
| `setLabelProvider(Function<T,String>)` | Como o item vira texto; padrao `String::valueOf` |
| `setCellRenderer(ListCellRenderer<? super T>)` | Renderizador customizado para as duas listas |
| `setComparator(Comparator<T>)` | Mantem ordenado; `null` preserva a ordem de insercao |
| `setMaxSelected(int)` | Limite de selecionados; negativo remove o limite |

`setMaxSelected` tambem corta o excesso ja selecionado, devolvendo os itens para a lista de disponiveis.

## Transferencia

| Metodo | Contrato |
|---|---|
| `addSelection()` | Move os itens marcados na lista da esquerda |
| `addAllItems()` | Move todos os itens **visiveis** na esquerda |
| `removeSelection()` | Devolve os itens marcados na lista da direita |
| `removeAllItems()` | Devolve todos os itens visiveis na direita |
| `moveUp()` / `moveDown()` | Reordena a selecao da direita |

Os metodos "all" respeitam o filtro corrente: movem o que esta visivel, nao o modelo inteiro. A reordenacao so age quando `setReorderable(true)` e quando nao ha filtro ativo escondendo itens.

Alem dos botoes, o duplo clique e a tecla `Enter` transferem o item sob o cursor ou a selecao corrente.

## Visual e comportamento

| Metodo | Uso |
|---|---|
| `setTitles(String left, String right)` | Titulos acima das listas |
| `setShowTitles(boolean)` | Exibe ou oculta os titulos |
| `setShowCounters(boolean)` | Mostra a contagem no titulo (`Selecionados (2/3)`) |
| `setShowFilter(boolean)` | Campo de filtro no topo de cada lista |
| `setReorderable(boolean)` | Habilita os botoes de subir e descer |
| `setRowHeight(int)` | Altura de cada linha |
| `getAvailableList()` / `getSelectedList()` | Acesso as `JList` internas |

Cores, fontes e espacamento vem de [UiTokens](UiTokens.md).

## Eventos

| Evento | Quando |
|---|---|
| `EventType.CHANGE` | Qualquer alteracao no conjunto selecionado |
| `DualListField.ITEMS_ADDED` | Itens foram para a direita |
| `DualListField.ITEMS_REMOVED` | Itens voltaram para a esquerda |
| `DualListField.ORDER_CHANGED` | A ordem dos selecionados mudou |

O valor do evento e a lista corrente de selecionados. As propriedades trazem `oldValue`, `newValue` e, conforme o caso, `moved`, `added`, `removed` ou `offset`.

```java
perfis.addEventListener(DualListField.ITEMS_ADDED, event -> {
    List<String> movidos = event.tryGetValue();
    List<String> antes = (List<String>) event.getProperties().get("oldValue");
});
```

## Internacionalizacao

Titulos, placeholder do filtro e tooltips dos botoes passam por `I18n.getText(DualListField.class, ...)`. As chaves estao em `src/main/resources/languages/{pt-BR,en-US,es-ES}.json` sob o prefixo `DualListField.`.

## Em formularios

`FormValues` sabe ler e escrever um `DualListField`, entao ele funciona direto dentro de um [FormPanel](FormPanel.md):

```java
form.addField(new FormField("perfis", "Perfis", perfis).setRequired(true));
```

`Validators.required()` considera lista vazia como valor ausente.
