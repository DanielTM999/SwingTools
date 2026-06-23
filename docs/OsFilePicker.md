# OsFilePicker

`OsFilePicker` e uma fachada estatica para abrir o seletor nativo de arquivos do sistema operacional.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.osfilepicker` |
| Tipo | API estatica |
| Filtro | `DeFilter` |
| Loader | `OsFilePickerNativeLoader` |

## API

| Metodo | Retorno |
|---|---|
| `openFile(String, DeFilter...)` | `File` |
| `openFiles(String, DeFilter...)` | `File[]` |
| `saveFile(String, String, DeFilter...)` | `File` |
| `openDirectory(String)` | `File` |
| `openFileOrDirectory(String, DeFilter...)` | `File` |
| `openFilesOrDirectories(String, DeFilter...)` | `File[]` |

Todos os metodos tambem possuem overload com `File initialDir`.

## Exemplo

```java
DeFilter images = DeFilter.of("Imagens", "png", "jpg", "jpeg");
File selected = OsFilePicker.openFile("Abrir imagem", new File("C:/"), images);
```

## Cuidados

- Depende dos binarios nativos em `src/main/resources/native`.
- Use `-Dnative.build.skip=true` quando os binarios ja estiverem empacotados e voce nao quiser rebuild nativo.
- O retorno pode ser `null` quando o usuario cancela.
