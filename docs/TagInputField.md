# TagInputField

`TagInputField` e um campo para entrada de multiplas tags com validacao, normalizacao e render customizado.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.tagfield` |
| Heranca | `TagInputField extends PanelEventListener` |
| Eventos proprios | `TAG_ADD`, `TAG_REMOVE`, `TAG_CLICK` |

## API de dados

| Metodo | Uso |
|---|---|
| `addTag(String)` / `addTag(String, boolean)` | Adiciona tag |
| `addTags(Collection<String>)` | Adiciona varias |
| `removeTag(String)` / `removeTagAt(int)` | Remove |
| `removeLastTag()` | Remove ultima |
| `clearTags()` | Limpa |
| `setTags(Collection<String>)` | Substitui |
| `getTags()` | Lista atual |
| `getText()` / `setText(String)` | Texto de entrada |

## Regras

| Metodo | Uso |
|---|---|
| `setTagValidator(Predicate<String>)` | Valida tag |
| `setTagNormalizer(UnaryOperator<String>)` | Normaliza |
| `setAllowDuplicates(boolean)` | Permite duplicadas |
| `setCaseSensitiveDuplicates(boolean)` | Compara duplicadas com case |
| `setCommitOnFocusLost(boolean)` | Confirma ao perder foco |
| `setMaxTags(int)` | Limite |
| `setSeparatorsRegex(String)` | Separadores |

## Visual

`setTagRenderer`, `setPlaceholder`, `setAddButtonVisible`, `setRemoveButtonVisible`, `setTagColors`, `setTagRemoveForeground`.

## Exemplo

```java
import dtm.stools.component.inputfields.tagfield.TagInputField;

TagInputField tags = new TagInputField()
        .setTagNormalizer(String::trim)
        .setTagValidator(tag -> tag.length() >= 2)
        .setAllowDuplicates(false);

tags.addEventListener(TagInputField.TAG_ADD, event -> {
    System.out.println(event.getValue());
});

tags.addTags(java.util.List.of("java", "swing"));
java.util.List<String> selecionadas = tags.getTags();
```

Defina normalizador, validador e limite antes de preencher dados iniciais. Use `setTags` para substituir a seleção inteira e `getTags` ao salvar o formulário. `TAG_ADD` e `TAG_REMOVE` são adequados para atualizar uma tela dependente das tags; a persistência final pode acontecer no submit.
