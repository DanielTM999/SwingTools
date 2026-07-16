# AbstractGraphicsPanel

`AbstractGraphicsPanel<C extends GraphicsContext>` e a base comum para paineis graficos do SwingTools. Ela e um `JPanel`, mas nao desenha diretamente via `paintComponent`; a responsabilidade dela e definir um contrato para implementacoes graficas que precisam de renderer, contexto, loop de render, FPS, VSync, input e descarte controlado.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.graphics` |
| Heranca | `AbstractGraphicsPanel<C extends GraphicsContext> extends JPanel` |
| Tipo | Classe abstrata |
| Uso principal | Base para implementacoes graficas, como `GraphicsGlPanel` |

## Papel da classe

`AbstractGraphicsPanel` existe para que o restante da aplicacao use paineis graficos de forma uniforme, mesmo que cada backend tenha um contexto diferente. O painel concreto decide como criar superficie, thread e contexto; o codigo consumidor usa os mesmos metodos para configurar renderer, FPS, input e descarte.

O tipo generico `C` representa o contexto entregue ao renderer. Em OpenGL, esse contexto e `GraphicsGlContext`; em outro backend poderia ser um contexto diferente, desde que implemente `GraphicsContext`.

## Contratos principais

| Tipo | Uso |
|---|---|
| `GraphicsContext` | Contexto recebido pelo renderer; expoe tamanho, input e `runOnUiThread` |
| `GraphicsRender<C>` | Renderer com callbacks `initialize`, `render`, `resize` e `dispose` |
| `GraphicsInput` | Snapshot consultavel de teclado, mouse e scroll |
| `RenderThreadingMode` | Define se o loop usa scheduler compartilhado ou individual |

## Ciclo de vida esperado

```text
new panel
  setRenderer(...)
  setFPS(...) / setVsync(...) / setRenderMode(...)
  add ao container Swing
  init automatico pelo backend ou init() manual
  renderer.initialize(context)
  renderer.resize(context, width, height)
  renderer.render(context) a cada frame
  renderer.dispose(context)
  panel.dispose()
```

O ponto exato em que o contexto e criado depende da implementacao. Em `GraphicsGlPanel`, isso acontece quando o `Canvas` interno ja esta displayable e o scheduler executa o primeiro frame valido.

## API

| Metodo | Uso |
|---|---|
| `setRenderer(GraphicsRender<C>)` | Define ou troca o renderer ativo |
| `getRenderer()` | Retorna o renderer atualmente configurado |
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
| `init()` | Inicia ou registra o painel manualmente quando aplicavel |
| `dispose()` | Libera recursos graficos |

## Estados

| Estado | Significado pratico |
|---|---|
| `isContextCreated()` | A superficie/contexto grafico ja existe. Ainda nao garante que o renderer inicializou com sucesso. |
| `isReady()` | Contexto criado e renderer inicializado. E o estado ideal para desenho normal. |
| `isRendering()` | O loop esta registrado e ativo. |
| `isDisposed()` | O painel foi descartado e nao deve ser reutilizado. |

`isReady()` e util para UI de status. Para a maioria dos renderers, prefira inicializar recursos dentro de `initialize(...)`, nao consultando `isReady()` de fora.

## Renderer

```java
class MyRender implements GraphicsRender<MyContext> {
    @Override
    public void initialize(MyContext context) {
        // Criar recursos dependentes do contexto grafico.
    }

    @Override
    public void render(MyContext context) {
        // Desenhar um frame.
    }

    @Override
    public void resize(MyContext context, int width, int height) {
        // Ajustar viewport, buffers, camera ou recursos dependentes de tamanho.
    }

    @Override
    public void dispose(MyContext context) {
        // Liberar recursos criados em initialize.
    }
}
```

Regras praticas:

- `initialize(...)` deve criar recursos ligados ao contexto.
- `render(...)` deve desenhar um frame e evitar trabalho bloqueante.
- `resize(...)` deve ajustar estado dependente de tamanho.
- `dispose(...)` deve ser idempotente o suficiente para tolerar descarte apos falha parcial de inicializacao.
- `getRenderer()` retorna a instancia configurada no painel; isso nao garante que ela ja tenha passado por `initialize(...)`.

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

`GraphicsInput` e uma interface de consulta. A implementacao atual, `GraphicsInputState`, recebe eventos AWT/Swing e mantem sets concorrentes de teclas e botoes pressionados. Em perda de foco, teclas e botoes sao limpos para evitar estado preso.

`getWheelRotation()` e acumulado. Para zoom absoluto, use o valor diretamente. Para delta por frame, guarde o valor anterior no renderer.

## Threading

| Modo | Uso |
|---|---|
| `RenderThreadingMode.SHARED` | Scheduler compartilhado entre paineis; e o padrao |
| `RenderThreadingMode.INDIVIDUAL` | Loop proprio para o painel |

Chame `setRenderMode(...)` antes de o painel entrar em renderizacao. A base impede trocar o modo enquanto `isRendering()` for `true`.

`runOnUiThread(...)` nao significa EDT do Swing. Neste modulo, "UI thread" e a thread do contexto grafico/backend. Em `GraphicsGlPanel`, e a thread onde o contexto OpenGL esta atual. Continue usando `SwingUtilities.invokeLater(...)` para alterar componentes Swing.

## Quando usar

Use `AbstractGraphicsPanel` como tipo de API quando seu codigo nao precisa saber qual backend grafico esta por baixo:

```java
void configure(AbstractGraphicsPanel<?> panel) {
    panel.setFPS(60);
    panel.setVsync(true);
}
```

Use a classe concreta (`GraphicsGlPanel`) quando precisar acessar contexto, helpers ou recursos especificos do backend.

## Cuidados

- Crie e adicione componentes Swing na EDT.
- Libere recursos graficos em `dispose()`.
- Prefira alterar recursos graficos dentro do renderer. Workers podem preparar dados, mas a acao de desenho deve ficar em `render(...)`. Use `runOnUiThread(...)` apenas como recurso de escape quando algum codigo externo saiu do renderer e realmente precisar executar uma tarefa curta no contexto grafico.
- Configure `setFPS(...)`, `setVsync(...)` e `setRenderMode(...)` antes de mostrar a janela quando possivel.
- Nao chame APIs graficas dependentes de contexto a partir da EDT, a menos que a implementacao documente isso explicitamente.
