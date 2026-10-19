# DropdownField

`DropdownField` e um combo box com datasource flexivel, renderer configuravel e eventos.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.selectfield` |
| Heranca | `DropdownField extends DropdownFieldListener<Object> extends JComboBox<Object>` |
| Uso principal | Selecionar um item de uma colecao |

## Criacao

```java
import dtm.stools.component.inputfields.selectfield.DropdownField;

DropdownField status = new DropdownField("Novo", "Em andamento", "Fechado");
status.setPlaceholder("Selecione");
```

O valor selecionado segue a API de `JComboBox`: `getSelectedItem()` pode devolver `null`. Quando os itens são objetos da aplicação, configure `setDisplayText` para o rótulo e faça o cast somente após verificar o tipo do item escolhido.

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
| `setLabel(String)` / `getLabel()` | Rótulo do layout e nome acessível |
| `setFieldLayoutManager(FieldLayoutManager)` / `getFieldLayoutManager()` | Configura ou consulta o layout; `null` restaura o visual convencional |

## MaterialLayout

`DropdownFieldListener` implementa `FieldLayoutConfigurable`, também implementada por `JTextFieldListener`. O suporte é herdado por `DropdownField` e funciona nos modos normal e editável.

```java
DropdownField status = new DropdownField("Novo", "Em andamento", "Fechado");
status.setLabel("Status");
status.setPlaceholder("Selecione um status");
status.setFieldLayoutManager(new MaterialLayout());

FieldLayoutConfigurable field = status;
field.setFieldLayoutManager(new MaterialLayout().setError("Selecione um status."));
field.setFieldLayoutManager(null);
```

Importe `FieldLayoutConfigurable` e `MaterialLayout` de `dtm.stools.component.inputfields.textfield.layout`. Use uma instância de `MaterialLayout` por campo e configure na EDT.

Sem seleção, o dropdown normal é considerado vazio. No modo editável, o conteúdo atual do editor determina esse estado, mesmo antes de confirmar a seleção. O foco no editor eleva o rótulo. O placeholder do renderer aguarda o rótulo elevado para aparecer. Mensagens de erro permanecem até serem removidas explicitamente.

O layout preserva o renderer, a busca por digitação, o popup e os eventos de seleção. A apresentação convencional é restaurada ao passar `null`, e o texto pendente do editor é mantido durante a instalação, remoção e troca de tema.

O popup abre junto à borda visível do campo, sem o espaço reservado à mensagem de erro entre o campo e a lista. Quando não há espaço abaixo, a lista abre acima do campo.

Com FlatLaf, o popup mantém a borda, as margens, as cores, a seleção e o renderer do tema ativo, incluindo as configurações do `UIManager`. A seta aponta para cima enquanto a lista está aberta e volta a apontar para baixo ao fechar ou cancelar. A troca de tema reaplica essa apresentação também no modo editável.

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
status.addEventListener(EventType.CHANGE, event -> {
    Object selected = event.getValue();
});
```

Tambem herda `LOAD` de `DropdownFieldListener`.

## Cuidados

- O tipo publico do componente e `Object`; use `getValue(Class<T>)` no listener base ou cast controlado quando precisar de tipo.
- `select(Object)` compara por `Objects.equals`.
- Chame `setSelectedItem(null)` se quiser deixar sem selecao apos trocar datasource manualmente.
