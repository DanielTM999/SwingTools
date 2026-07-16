# AbstractGraphicsPanel

`AbstractGraphicsPanel<C extends GraphicsContext>` e a base comum para paineis graficos do SwingTools. Ela separa o componente Swing do renderer e padroniza loop de render, FPS, VSync, input e ciclo de vida.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.graphics` |
| Heranca | `AbstractGraphicsPanel<C extends GraphicsContext> extends JPanel` |
| Tipo | Classe abstrata |
| Uso principal | Base para implementacoes graficas como `GraphicsGlPanel` |

## Contratos principais

| Tipo | Uso |
|---|---|
| `GraphicsContext` | Contexto recebido pelo renderer; expõe tamanho, input e `runOnUiThread` |
| `GraphicsRender<C>` | Renderer com callbacks `initialize`, `render`, `resize` e `dispose` |
| `GraphicsInput` | Snapshot consultavel de teclado, mouse e scroll |
| `RenderThreadingMode` | Define se o loop usa scheduler compartilhado ou individual |

## API

| Metodo | Uso |
|---|---|
| `setRenderer(GraphicsRender<C>)` | Define o renderer ativo |
| `runOnUiThread(Runnable)` | Enfileira trabalho para a thread do contexto grafico |
| `getInput()` | Retorna estado de teclado, mouse e scroll |
| `setRenderMode(RenderThreadingMode)` | Define `SHARED` ou `INDIVIDUAL`; precisa ser chamado antes de renderizar |
| `getRenderMode()` | Retorna o modo de threading atual |
| `setVsync(boolean)` / `isVsync()` | Controla VSync quando suportado pela implementacao |
| `setFPS(int)` / `getFPS()` | Define e le o alvo de FPS |
| `getCurrentFPS()` | FPS medido no contexto atual |
| `isReady()` | Indica renderer pronto para desenhar |
| `isContextCreated()` | Indica contexto grafico criado |
| `isRendering()` | Indica loop ativo |
| `isDisposed()` | Indica painel/contexto descartado |
| `init()` | Inicia ou registra o painel manualmente |
| `dispose()` | Libera recursos graficos |

## Renderer

```java
class MyRender implements GraphicsRender<MyContext> {
    @Override
    public void initialize(MyContext context) {
        // Criar recursos dependentes do contexto.
    }

    @Override
    public void render(MyContext context) {
        // Desenhar um frame.
    }

    @Override
    public void resize(MyContext context, int width, int height) {
        // Ajustar viewport, buffers ou camera.
    }

    @Override
    public void dispose(MyContext context) {
        // Liberar recursos criados em initialize.
    }
}
```

## Input

`GraphicsInput` e obtido por `context.getInput()` ou `panel.getInput()`:

```java
GraphicsInput input = context.getInput();
if (input.isKeyDown(KeyEvent.VK_SPACE)) {
    // Acao durante o frame atual.
}
if (input.isMouseButtonDown(MouseEvent.BUTTON1)) {
    int x = input.getMouseX();
    int y = input.getMouseY();
}
```

## Threading

| Modo | Uso |
|---|---|
| `RenderThreadingMode.SHARED` | Scheduler compartilhado entre paineis; e o padrao |
| `RenderThreadingMode.INDIVIDUAL` | Loop proprio para o painel |

Chame `setRenderMode(...)` antes de o painel entrar em renderizacao. A base impede trocar o modo enquanto `isRendering()` for `true`.

## Cuidados

- Crie e adicione componentes Swing na EDT.
- Libere recursos graficos em `dispose()`.
- Use `runOnUiThread(...)` para alterar recursos que pertencem ao contexto grafico a partir de listeners Swing ou workers.
- Configure `setFPS(...)`, `setVsync(...)` e `setRenderMode(...)` antes de mostrar a janela quando possivel.
