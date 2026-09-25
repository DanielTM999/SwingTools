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
import dtm.stools.component.inputfields.osfilepicker.DeFilter;
import dtm.stools.component.inputfields.osfilepicker.OsFilePicker;

import java.io.File;

DeFilter images = DeFilter.of("Imagens", "png", "jpg", "jpeg");
File selected = OsFilePicker.openFile("Abrir imagem", images);
if (selected != null) {
    // Use o arquivo selecionado na aplicação.
    System.out.println(selected.getAbsolutePath());
}
```

Passe um `File initialDir` antes dos filtros quando quiser sugerir a pasta inicial. `openFiles` e `openFilesOrDirectories` retornam arrays; verifique `null` antes de iterar. O diálogo pode ser aberto sem filtros, e `DeFilter.of` recebe extensões sem `*.`.

## Cuidados

- Em aplicações consumidoras, os binários são carregados dos recursos do JAR para o sistema e a arquitetura atuais; se não houver recurso, o loader tenta `System.loadLibrary("osfilepicker")`.
- `-Dnative.build.skip=true` é opção de **build da SwingTools**, não uma configuração necessária para executar o seletor na aplicação.
- O retorno pode ser `null` quando o usuario cancela.
