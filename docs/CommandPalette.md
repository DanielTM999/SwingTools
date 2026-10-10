# CommandPalette

Paleta independente em `dtm.stools.component.command`, utilizável como painel ou janela modeless baseada em `DialogActivity`. O catálogo e a execução pertencem ao aplicativo.

```java
CommandPalette palette = new CommandPalette(
    () -> List.of(new CommandEntry(
        "app.save", "Salvar", "Arquivo", "Gravar o documento", "Ctrl+S", true)),
    id -> {
        // Executar o comando identificado e retornar sucesso.
        return true;
    });
palette.open(janela);
AutoCloseable shortcut = palette.installShortcut(
    janela.getRootPane(), KeyStroke.getKeyStroke("control shift P"));
// Ao descartar a integração:
shortcut.close();
palette.close();
```

`CommandEntry` contém `id`, `name`, `group`, `description`, `shortcut` e `enabled`. IDs são obrigatórios e únicos; campos descritivos nulos viram texto vazio. `shortcut` é informação visual: a paleta não instala atalhos individuais dos comandos.

| API | Contrato |
|---|---|
| Construtor com `Supplier<List<CommandEntry>>` e `Predicate<String>` | Catálogo consultado na abertura, atualização e antes da execução; callback retorna sucesso |
| `refresh()` | Reconsulta catálogo e aplica o filtro corrente |
| `open(Component)` | Abre uma única janela vinculada ao proprietário; reabrir atualiza e traz ao frente |
| `isOpen()` / `toFront()` / `close()` | Ciclo de vida da janela; fechar é idempotente e permite reabrir |
| `onClosed(Runnable)` | Callback de fechamento, chamado uma vez por janela |
| `installShortcut(JRootPane, KeyStroke)` | Instala atalho local e retorna registro removível, restaurando a associação anterior |
| `getSearchField()` / `getCommandList()` | Personalização e integração do painel |

A busca ignora caixa e acentos e consulta nome, grupo, descrição e ID. Mantém a ordem fornecida. Comandos desabilitados permanecem visíveis, mas não executam. Setas navegam, Enter e duplo clique executam, Esc fecha; lista vazia e falha de execução exibem mensagens. Sucesso fecha a janela e restaura o foco anterior. Ações devem ser breves na EDT; operações demoradas devem iniciar trabalho assíncrono no aplicativo.

Os providers padrão de Word e Sheet adaptam seus catálogos a este componente. As interfaces `WordCommandPaletteProvider` e `SheetCommandPaletteProvider`, os IDs padrão e os handles existentes continuam disponíveis. Um provider personalizado pode abrir outra interface sem depender desta paleta. O componente geral não importa classes de nenhum editor.

Exemplo executável: `dtm.stools.examples.CommandPaletteExample`.
