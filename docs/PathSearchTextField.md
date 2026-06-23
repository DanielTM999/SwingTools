# PathSearchTextField

`PathSearchTextField<T>` combina autocomplete de `SearchTextField<T>` com visual de caminho segmentado.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.textfield` |
| Heranca | `PathSearchTextField<T> extends SearchTextField<T>` |
| Evento proprio | `PATH_SEGMENT_CLICK` |

## Uso

```java
PathSearchTextField<Path> field = new PathSearchTextField<>("/");
field.setDataSource(paths);
field.setDisplayFunction(path -> path.toString());
```

## API propria

| Metodo | Uso |
|---|---|
| `enterEditMode()` | Edita texto completo |
| `exitEditMode(boolean applyChanges)` | Finaliza edicao |
| `setSeparator(String)` | Define separador |
| `getSegments()` | Segmentos |
| `getPathList()` | Lista do caminho |

## Herdado

Toda API de `SearchTextField<T>` continua disponivel: datasource, estrategias de busca, popup, `EventType.SELECT`, `INPUT` e `CHANGE`.

## Cuidados

- O valor exibido vem de `setDisplayFunction`; mantenha formato compativel com o separador.
- Para busca de sistema de arquivos grande, atualize datasource sob demanda.
