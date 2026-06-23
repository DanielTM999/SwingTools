# GridViewTable

`GridViewTable<T>` e uma tabela Swing que monta colunas a partir de um POJO anotado com `@GridColumn`.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.grids` |
| Heranca | `GridViewTable<T> extends DataTableListener extends JTable` |
| Modelo interno | `ReflectionTableModel<T>` |
| Uso principal | Exibir e editar colecoes de objetos com pouca configuracao manual |

## Heranca

```text
JTable
  DataTableListener
    GridViewTable<T>
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
| `setterRef` | Nome de setter alternativo para escrita |

## Criacao e datasource

```java
GridViewTable<UserRow> table = new GridViewTable<>(UserRow.class);
table.setDataSource(users);
table.setGridMode(TableGridMode.SINGLE);
table.setAllowEdit(true);
```

| Metodo | Contrato |
|---|---|
| `GridViewTable(Class<T>)` | Cria em modo padrao |
| `GridViewTable(Class<T>, TableGridMode)` | Cria ja definindo modo |
| `setDataSource(Collection<T>)` | Troca os dados |
| `setGridMode(TableGridMode)` | Define selecao `SINGLE` ou `BATCH` |
| `setAllowEdit(boolean)` | Habilita/desabilita edicao |
| `getRow(int)` | Retorna valores da linha como lista |
| `getRowObject(int)` | Retorna o objeto `T` da linha |

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

## Eventos

```java
table.addEventListner(EventGridViewTable.SELECTION_ROW, event -> {
    UserRow row = table.getRowObject(table.getSelectedRow());
});

table.addEventListner(EventGridViewTable.CELL_EDIT, event -> {
    System.out.println(event.getProperties());
});
```

Eventos especializados ficam em `EventGridViewTable`.

## Exemplo completo

```java
JFrame frame = new JFrame("Usuarios");
GridViewTable<UserRow> table = new GridViewTable<>(UserRow.class, TableGridMode.SINGLE);

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
