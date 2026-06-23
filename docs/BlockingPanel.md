# BlockingPanel

`BlockingPanel` e um `ViewPanel` capaz de bloquear interacao do usuario com seus filhos.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels` |
| Heranca | `BlockingPanel extends ViewPanel implements IWindowComponent` |
| Uso principal | Impedir clique, teclado e movimento de mouse durante carregamentos ou operacoes criticas |

## Como funciona

O painel cria um `JComponent` transparente interno que fica na frente dos filhos quando o painel esta bloqueado. Esse componente consome eventos de mouse e teclado. Quando o painel e desbloqueado, o bloqueador fica invisivel.

```text
ViewPanel
  BlockingPanel
    PanelEventListener
      KeyPanel
      TabbedPanel
      DockPanel
```

## API principal

| Metodo | Contrato |
|---|---|
| `lockUI()` | Bloqueia a interacao se ainda nao estiver bloqueado |
| `lockUI(Consumer<BlockingPanel>)` | Bloqueia e executa callback uma vez |
| `unlockUI()` | Desbloqueia se estiver bloqueado |
| `unlockUI(Consumer<BlockingPanel>)` | Desbloqueia e executa callback uma vez |
| `isLocked()` | Retorna `true` quando a UI esta bloqueada |

## Exemplo

```java
BlockingPanel panel = new BlockingPanel(new BorderLayout()) {};
panel.add(new JButton("Salvar"), BorderLayout.SOUTH);

panel.lockUI(p -> {
    p.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
});

// Ao terminar o trabalho:
panel.unlockUI(p -> {
    p.setCursor(Cursor.getDefaultCursor());
});
```

## Uso com tarefa em background

```java
panel.lockUI();

CompletableFuture
        .supplyAsync(service::loadData)
        .whenComplete((data, error) -> SwingUtilities.invokeLater(() -> {
            try {
                if (error != null) {
                    JOptionPane.showMessageDialog(panel, error.getMessage());
                    return;
                }
                render(data);
            } finally {
                panel.unlockUI();
            }
        }));
```

## Cuidados

- Nao use como controle de permissao; ele e apenas uma protecao visual/interativa.
- O bloqueio nao cancela tarefas em background.
- Alteracoes de UI devem voltar para a EDT.
- Componentes adicionados depois do bloqueio sao reposicionados atras do bloqueador automaticamente pelo `add`.
