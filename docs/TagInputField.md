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
TagInputField tags = new TagInputField()
        .setTagNormalizer(String::trim)
        .setTagValidator(tag -> tag.length() >= 2)
        .setAllowDuplicates(false);

tags.addEventListner(TagInputField.TAG_ADD, event -> {
    System.out.println(event.getValue());
});
```
