# SearchTextField

`SearchTextField<T>` e um campo de texto com datasource, busca e popup de sugestoes.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.textfield` |
| Heranca | `SearchTextField<T> extends MaskedTextField` |
| Uso principal | Autocomplete, busca em lista local, selecao rapida de objeto |

## Dados e exibicao

```java
SearchTextField<User> field = new SearchTextField<>();
field.setDataSource(users);
field.setDisplayFunction(User::name);
field.addSearchOption(User::email);
```

| Metodo | Contrato |
|---|---|
| `setDataSource(List<T>)` | Define a lista pesquisavel |
| `addSearchOption(Function<T, String>)` | Adiciona campo de busca alem do texto principal |
| `setDisplayFunction(Function<T, String>)` | Texto exibido no campo e no popup |
| `getSelectedSuggestion()` | Ultimo item selecionado |

## Busca

| Metodo | Uso |
|---|---|
| `setMinLength(int)` | Minimo de caracteres para buscar |
| `setMaxResults(int)` | Limite de sugestoes |
| `setSearchStrategy(BiPredicate<String, String>)` | Comparacao customizada |
| `useStartsWithSearch()` | Busca por prefixo |
| `useExactSearch()` | Busca exata |
| `setCaseSensitive(boolean)` | Sensibilidade a maiusculas/minusculas |
| `sortResultsAlphabetically()` | Ordena sugestoes |

## Popup

| Metodo | Uso |
|---|---|
| `showAllSuggestions()` | Mostra todas as sugestoes |
| `closePopup()` | Fecha o popup |
| `clearAndClose()` | Limpa texto e fecha |
| `setShowPopup(boolean)` | Liga/desliga popup |
| `setPopupHeight(int)` | Altura do popup |
| `setAutoCloseOnFocusLost(boolean)` | Fecha ao perder foco |
| `ignoreCurrentSugestions()` | Ignora sugestoes atuais |

## Eventos

`EventType.SELECT` e disparado quando o usuario escolhe uma sugestao. Eventos herdados de `MaskedTextField`, como `INPUT` e `CHANGE`, continuam disponiveis.

```java
field.addEventListner(EventType.SELECT, event -> {
    User selected = event.tryGetValue();
    openUser(selected);
});
```

## Cuidados

- A busca e local na lista informada. Para busca remota, atualize o datasource de forma controlada.
- O popup e Swing; atualize dados na EDT.
- Para grandes listas, reduza `maxResults` e use estrategia de busca eficiente.
