# GridView

`GridView<T>` é uma tabela Swing que monta colunas a partir de um POJO anotado com `@GridColumn`. Os campos são filtrados e ordenados antes da paginação.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.grids` |
| Heranca | `GridView<T> extends DataTableListener extends JTable` |
| Modelo interno | `ReflectionTableModel<T>` |
| Uso principal | Exibir e editar colecoes de objetos com pouca configuracao manual |

## Heranca

```text
JTable
  DataTableListener
    GridView<T>
```

Por herdar `DataTableListener`, a tabela implementa `EventListenerComponent` e emite eventos de selecao e edicao.

## Modelo de dados

Anote os campos que devem virar colunas:

```java
public class UserRow {
    @GridColumn(name = "ID", order = 1, width = 80, editable = false)
    private Long id;

    @GridColumn(name = "Nome", order = 2, width = 220)
    private String name;

    @GridColumn(name = "Ativo", order = 3)
    private Boolean active;
}
```

`@GridColumn`:

| Atributo | Uso |
|---|---|
| `name` | Titulo no cabecalho |
| `order` | Ordem da coluna |
| `editable` | Se a celula pode ser editada |
| `width` | Largura preferencial |
| `visible` | Se a coluna aparece |
| `setterRef` | Nome de campo alternativo para escrita |

## Criacao e datasource

```java
GridView<UserRow> table = new GridView<>(UserRow.class);
table.setDataSource(users);
table.setGridMode(TableGridMode.SINGLE);
table.setAllowEdit(true);
```

| Metodo | Contrato |
|---|---|
| `GridView(Class<T>)` | Cria em modo padrao |
| `GridView(Class<T>, TableGridMode)` | Cria ja definindo modo |
| `setDataSource(Collection<T>)` | Troca os dados |
| `refreshData()` | Reaplica filtros e ordenação após alterar objetos diretamente |
| `setGridMode(TableGridMode)` | Define selecao `SINGLE` ou `BATCH` |
| `setAllowEdit(boolean)` | Chave geral de edição de células e formulários |
| `getRow(int)` | Retorna valores da linha como lista |
| `getRowObject(int)` | Retorna o objeto `T` da linha |

Os índices recebidos por `getRow` e `getRowObject` são os da visão atual. O componente aplica `width` e `visible` da anotação ao criar as colunas. Para trocar o modelo diretamente, use um `ReflectionTableModel` do mesmo tipo; outros modelos são rejeitados com uma exceção clara.

### Bloquear edição direta na grade

Com `setAllowEdit(true)`, as células editáveis vêm habilitadas por padrão. Para manter o formulário de linha disponível e impedir a edição por clique na célula:

```java
table.setAllowEdit(true);
table.setCellEditingEnabled(false); // o formulário continua disponível
```

Também é possível controlar a edição direta por coluna, por objeto de linha ou por uma célula específica:

```java
table.setColumnCellEditingEnabled("price", false);
table.setRowCellEditingEnabled(order, false);
table.setCellEditingEnabled(order, "status", false);
```

Passe `true` aos mesmos métodos para remover cada bloqueio. Esses controles afetam apenas os editores de células do `JTable`; o formulário continua seguindo `@GridColumn(editable=...)` e a chave geral `setAllowEdit`. Os bloqueios de linha usam a identidade do objeto, mesmo após ordenar ou paginar.

## Estilos

Os estilos internos dispensam um renderer externo para mudar cores, fonte, alinhamento e bordas. Propriedades `null` de `GridCellStyle` herdam a camada anterior.

```java
table.setGridStyle(GridStyle.standard().withStriped(true).withRowHeight(30));
table.setColumnStyle("name", GridCellStyle.empty().withAlignment(SwingConstants.LEADING));
table.setRowStyle(users.getFirst(), GridCellStyle.empty().withBackground(new Color(0xF0F7FF)));
table.setCellStyle(users.getFirst(), "name", GridCellStyle.empty().withForeground(Color.BLUE));
table.setStyleResolver((user, field, value) ->
        "active".equals(field) && Boolean.FALSE.equals(value)
                ? GridCellStyle.empty().withForeground(Color.GRAY) : null);
```

A prioridade é: aparência geral, coluna, linha, célula e resolver dinâmico. A seleção usa as cores de seleção para manter o texto legível. `GridStyle.standard()`, `compact()` e `plain()` são pontos de partida; valores não configurados acompanham `UiTokens`. Renderers Swing definidos explicitamente continuam tendo prioridade.

## Filtros e ordenação

```java
table.setColumnTextFilter("name", "ana"); // contém, sem diferenciar maiúsculas
table.setColumnFilter("id", value -> value instanceof Number n && n.longValue() >= 100);
table.setSort("name", SortOrder.ASCENDING);
table.clearColumnFilter("name");
table.clearFilters();
table.clearSort();
```

Clique no cabeçalho para alternar entre ordem crescente e decrescente. O botão direito abre o filtro textual da coluna. `getTotalItems()` conta a fonte completa; `getFilteredItems()` conta as linhas após os filtros. A ordenação considera todos os registros filtrados, inclusive os que não estão na página visível.

## Paginacao

```java
table.setPaginationEnabled(true);
table.setPageSize(25);
table.setPageSizeOptions(List.of(10, 25, 50, 100));
table.goToPage(1);
```

| Metodo | Uso |
|---|---|
| `getTotalPages()` | Total de paginas |
| `getTotalItems()` | Total de itens |
| `nextPage()` / `previousPage()` | Navegacao |
| `hasNextPage()` / `hasPreviousPage()` | Estado de navegacao |

`getPaginationPanel()` fornece um painel pronto com navegação, contagem e escolha do tamanho da página. Coloque-o abaixo do `JScrollPane` que contém a tabela; o painel fica visível quando `setPaginationEnabled(true)` for usado.

```java
JPanel container = new JPanel(new BorderLayout());
container.add(new JScrollPane(table), BorderLayout.CENTER);
container.add(table.getPaginationPanel(), BorderLayout.SOUTH);
table.setPaginationEnabled(true);
```

## Eventos

```java
table.addEventListener(EventGridView.SELECTION_ROW, event -> {
    UserRow row = table.getRowObject(table.getSelectedRow());
});

table.addEventListener(EventGridView.CELL_EDIT, event -> {
    EventGrid edit = event.tryGetValue();
    System.out.println(edit.getOldValue() + " -> " + edit.getNewValue());
});
```

Eventos especializados ficam em `EventGridView`.

## Formulário e ações de linha

Por padrão, não há coluna de ações. Habilite a edição por formulário escolhendo a apresentação:

```java
table.setAllowEdit(true);
table.setRowFormMode(GridRowFormMode.DIALOG); // ou INLINE
table.openRowForm(table.getSelectedRow());
```

`DIALOG` usa uma janela modal com Salvar e Cancelar. `INLINE` expande um formulário abaixo da linha. `OFF` desativa o formulário. A coluna Ações é criada pela grade e não precisa de `@GridColumn` nem de um renderer próprio. Quando só existe Editar, o clique abre o formulário; com ações adicionais, abre um menu. `setAllowEdit(false)` desabilita Editar, mas não desabilita as ações personalizadas.

O formulário padrão usa os campos visíveis anotados. Campos `editable=false` aparecem para leitura; campos ocultos não aparecem. Texto, números, booleanos e enums têm editores automáticos. Objetos com campos anotados podem ser expandidos; tipos sem editor ou propriedades anotadas aparecem para leitura. Para substituir o formulário padrão:

```java
table.setRowFormFactory((row, grid) -> {
    JTextField name = new JTextField(row.name);
    JPanel panel = new JPanel();
    panel.add(name);
    return new GridRowForm(panel, () -> Map.of("name", name.getText()));
});
table.setRowFormValidator((row, values) -> {
    if (String.valueOf(values.get("name")).isBlank())
        throw new IllegalArgumentException("Informe o nome.");
});
table.setRowSaveHandler((row, before, after) -> repository.save(row));
```

Os valores do formulário usam os nomes dos campos anotados. Ao salvar, a grade converte e valida todos os valores antes de alterar o objeto existente. Se a validação ou o callback de salvamento falhar, o formulário permanece aberto e o objeto mantém os valores anteriores. O callback é síncrono. `EventGridView.ROW_EDIT_SAVED` entrega um `GridRowEdit<T>` com a linha e os mapas anterior/novo; as células modificadas também emitem `CELL_EDIT`.

É possível incluir comandos próprios na mesma coluna:

```java
table.setRowActions(List.of(
    new GridRowAction<>("details", "Detalhes", row -> showDetails(row))
));
```

Uma ação também pode receber um `Predicate<T>` para definir em quais linhas estará habilitada. Ações executadas emitem `EventGridView.ROW_ACTION` com seu identificador. Quando um formulário inline está aberto, mudar página, filtro ou fonte de dados pode ocultá-lo; alterações ainda não salvas são descartadas.

### Objetos dentro do modelo

Campos de tipos próprios continuam sendo mostrados na grade pelo `toString()`. Quando a classe do objeto também usa `@GridColumn`, o formulário padrão permite abrir suas propriedades com **Propriedades**. A edição de uma propriedade modifica a instância já associada à linha; o seletor troca a referência quando outra instância é escolhida. Apenas campos anotados e visíveis são incluídos. `editable=false` mantém o campo somente para leitura.

```java
class Pedido {
    @GridColumn(name = "Cliente") Customer customer;
}
class Customer {
    @GridColumn(name = "Nome") String name;
    @GridColumn(name = "Endereço") Address address;
    @Override public String toString() { return name; }
}
class Address {
    @GridColumn(name = "Cidade") String city;
}

GridView<Pedido> table = new GridView<>(Pedido.class);
table.setAllowEdit(true);
table.setRowFormMode(GridRowFormMode.DIALOG);
table.setObjectChoices("customer", context -> availableCustomers);
table.setObjectFactory("customer", context -> new Customer());
```

`setObjectChoices` recebe o caminho do campo e um provedor chamado ao montar o formulário. O contexto contém a linha, o objeto pai, o caminho, o valor atual e o tipo esperado. O seletor também permite limpar o valor, se o campo aceitar `null`. Sem provedor, a referência existente permanece no formulário para editar suas propriedades. Para criar um objeto ausente, **Criar** usa a fábrica registrada com `setObjectFactory`; na falta dela, tenta o construtor sem argumentos. Se nenhum estiver disponível, a operação informa o caminho e o tipo que precisam de uma fábrica.

`setNestedEditDepth(-1)` permite expansão sem limite e é o padrão. `0` mostra apenas os campos da linha; `1` permite abrir os objetos diretamente associados a ela; `2` permite mais um nível. Referências cíclicas não são expandidas. O formulário próprio pode enviar caminhos como `customer.address.city` no mapa de valores; todos os trechos do caminho precisam estar anotados e editáveis. A grade valida as alterações antes de aplicá-las, desfaz as escritas se o callback de salvamento falhar e informa o caminho em `EventGrid.getFieldPath()` nos eventos `CELL_EDIT`.

Com `setCellEditingEnabled(true)`, a edição direta de uma célula que contém objeto abre o seletor e o formulário de propriedades. Os bloqueios de edição direta por linha e coluna continuam valendo. O [exemplo executável](../src/test/java/dtm/stools/examples/GridViewExample.java) mostra fornecedor, endereço, seletor de instâncias e criação sob demanda.

## Exemplo completo

```java
JFrame frame = new JFrame("Usuarios");
GridView<UserRow> table = new GridView<>(UserRow.class, TableGridMode.SINGLE);

table.setDataSource(List.of(
        new UserRow(1L, "Ana", true),
        new UserRow(2L, "Bruno", false)
));

frame.add(new JScrollPane(table));
frame.setSize(700, 400);
frame.setLocationRelativeTo(null);
frame.setVisible(true);
```

## Cuidados

- A classe de modelo precisa permitir leitura dos campos pelo modelo reflexivo.
- Se habilitar edicao, valide se o setter ou o campo suporta o tipo recebido do editor Swing.
- Para grandes volumes, use paginacao ou alimente a tabela com subconjuntos.
- Como e `JTable`, customizadores Swing como renderers e editors continuam funcionando.
