# DropdownField

`DropdownField` e um combo box com datasource flexivel, renderer configuravel e eventos.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.selectfield` |
| Heranca | `DropdownField extends DropdownFieldListener<Object> extends JComboBox<Object>` |
| Uso principal | Selecionar um item de uma colecao |

## Criacao

```java
DropdownField status = new DropdownField("Novo", "Em andamento", "Fechado");
status.setPlaceholder("Selecione");
```

| Construtor | Uso |
|---|---|
| `DropdownField()` | Vazio |
| `DropdownField(Object... dataSource)` | Varargs |
| `DropdownField(Collection<T>)` | Colecao |

## API principal

| Metodo | Contrato |
|---|---|
| `setDataSource(Collection<T>)` / `setDataSource(Object...)` | Troca itens |
| `getDataSource()` | Lista de itens como `Object` |
| `getDataSource(Class<T>)` | Lista tipada |
| `select(Object)` | Seleciona valor se existir |
| `selectFirst()` / `selectLast()` | Seleciona primeiro/ultimo |
| `contains(Object)` | Verifica se existe |
| `clear()` | Limpa itens e dispara `CLEAR` |
| `reload()` | Reaplica renderer |
| `setTypeAheadSelectionEnabled(boolean)` | Liga/desliga selecao por digitacao |
| `isTypeAheadSelectionEnabled()` | Retorna se selecao por digitacao esta ativa |
| `isEmpty()` / `getItemSize()` | Estado |

## Display e renderer

```java
DropdownField users = new DropdownField(userList);
users.setDisplayText(user -> ((User) user).name());
users.setCustomRenderer(new UserRenderer());
```

Se `setCustomRenderer` for usado, ele substitui a renderizacao padrao. Sem renderer customizado, `setDisplayText` controla o texto.

## Selecao por digitacao

Por padrao, `DropdownField` destaca no popup o primeiro item compativel enquanto o usuario digita, sem alterar o valor selecionado de fato. A selecao so e confirmada quando o usuario pressiona Enter ou escolhe um item com o mouse.

```java
DropdownField status = new DropdownField("Novo", "Em andamento", "Fechado");
status.setTypeAheadSelectionEnabled(false); // desativa se quiser o comportamento padrao do JComboBox
```

Em combos editaveis (`setEditable(true)`), o texto digitado no editor e usado para procurar o item. Em combos nao editaveis, as teclas digitadas montam uma busca temporaria e abrem o popup quando houver correspondencia.

## Eventos

```java
status.addEventListner(EventType.CHANGE, event -> {
    Object selected = event.getValue();
});
```

Tambem herda `LOAD` de `DropdownFieldListener`.

## Cuidados

- O tipo publico do componente e `Object`; use `getValue(Class<T>)` no listener base ou cast controlado quando precisar de tipo.
- `select(Object)` compara por `Objects.equals`.
- Chame `setSelectedItem(null)` se quiser deixar sem selecao apos trocar datasource manualmente.
