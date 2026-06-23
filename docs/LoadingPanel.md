# LoadingPanel

`LoadingPanel` mostra um overlay de carregamento sobre um conteudo Swing.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.loading` |
| Heranca | `LoadingPanel extends PanelEventListener` |
| Uso principal | Bloquear ou sinalizar operacoes demoradas com mensagem, spinner e progresso |

## Criacao

```java
LoadingPanel loading = new LoadingPanel(contentPanel)
        .setMessage("Carregando")
        .setIndeterminate();
```

## API

| Metodo | Uso |
|---|---|
| `setContent(Component)` | Define o conteudo abaixo do overlay |
| `setLoading(boolean)` | Liga/desliga carregamento |
| `start()` / `stop()` | Atalhos para carregar/parar |
| `setMessage(String)` | Texto exibido |
| `setProgress(double)` | Progresso |
| `setIndeterminate()` | Progresso indeterminado |
| `setBlockInput(boolean)` | Bloqueia interacao com o conteudo |
| `setShowMessage(boolean)` | Mostra/oculta mensagem |
| `setShowProgressText(boolean)` | Mostra/oculta texto do progresso |

## Eventos

Dispara `START`, `STOP`, `PROGRESS` e `MESSAGE_CHANGE`, alem dos eventos herdados de `PanelEventListener`.

## Exemplo

```java
loading.start();
CompletableFuture.runAsync(service::load)
        .whenComplete((ok, error) -> SwingUtilities.invokeLater(loading::stop));
```

## Cuidados

- Atualize estado visual na EDT.
- Use `setBlockInput(true)` quando o usuario nao deve interagir durante a operacao.
