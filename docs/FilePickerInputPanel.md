# FilePickerInputPanel

`FilePickerInputPanel` e um seletor de arquivos feito em Swing, com navegacao visual, filtros e selecao simples ou multipla.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.filepicker` |
| Heranca | `FilePickerInputPanel extends PanelEventListener` |
| Modo | `FileSelectionMode` |

## Criacao

```java
FilePickerInputPanel picker = new FilePickerInputPanel(Path.of("C:/"));
picker.setMultiSelectionEnabled(true);
picker.setFileSelectionMode(FileSelectionMode.FILES_ONLY);
```

## API

| Metodo | Uso |
|---|---|
| `setOnEndSelection(Consumer<FilePickerInputPanel>)` | Callback final |
| `setMultiSelectionEnabled(boolean)` | Multiselecao |
| `setFileSelectionMode(FileSelectionMode)` | Arquivo/diretorio |
| `setConfirmSelectionVButtonText(String)` | Texto do botao |
| `setFileFilter(FileNameExtensionFilter)` | Filtro |
| `setShowHiddenFiles(boolean)` | Mostra ocultos |
| `setRequired(boolean)` | Exige selecao |
| `setAllowNewFileInput(boolean)` | Permite digitar novo arquivo |
| `setSelectedFile(String/File)` | Define selecao |
| `getSelectedFile()` | Primeiro selecionado |
| `getSelectedFiles()` | Todos selecionados |
| `dispose()` | Libera recursos/listeners |

## Exemplo

```java
picker.setOnEndSelection(p -> {
    File selected = p.getSelectedFile();
    if (selected != null) {
        open(selected);
    }
});
```

## Tipos auxiliares

`FileWrapper`, `FileTreeNode`, `FileWrapperTableModel`, renderers de lista, grid, tabela, arvore e tamanho de arquivo.

## Cuidados

- Para dialog nativo, use `OsFilePicker`.
- Para validar obrigatoriedade, combine `setRequired(true)` com o callback final.
