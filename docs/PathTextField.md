# PathTextField

`PathTextField` representa um caminho segmentado, funcionando como breadcrumb editavel.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.textfield` |
| Heranca | `PathTextField extends MaskedTextField` |
| Evento proprio | `PATH_SEGMENT_CLICK` |

## Criacao

```java
PathTextField path = new PathTextField("/");
path.setText("src/main/java");
```

## API

| Metodo | Uso |
|---|---|
| `enterEditMode()` | Mostra edicao textual |
| `exitEditMode(boolean applyChanges)` | Sai aplicando ou descartando |
| `setSeparator(String)` | Troca separador |
| `getSegments()` | Segmentos calculados |
| `getPathList()` | Lista do caminho |

## Eventos

```java
path.addEventListner(PathTextField.PATH_SEGMENT_CLICK, event -> {
    String segment = event.tryGetValue();
});
```

## Cuidados

- Use separador coerente com o dominio: `/`, `\\`, `.` ou outro.
- Como herda `MaskedTextField`, eventos de texto continuam disponiveis.
