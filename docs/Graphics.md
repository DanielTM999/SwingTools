# Graphics components

Este documento cobre o pacote `dtm.stools.component.panels.graphics` e sua implementacao OpenGL em `dtm.stools.component.panels.graphics.gl`.

O objetivo desse modulo e separar tres responsabilidades que normalmente ficam misturadas em paineis graficos Swing:

1. O componente Swing que entra na tela.
2. O renderer, que sabe criar recursos, desenhar frames, reagir a resize e liberar memoria.
3. A thread/contexto grafico, onde chamadas dependentes de contexto precisam acontecer.

## Visao geral

```text
AbstractGraphicsPanel<C extends GraphicsContext>
  GraphicsGlPanel
    GraphicsGlHost
      GraphicsGlCanvas
      GraphicsGlContext
      GraphicsInputState
      GraphicsGlUiSchedule
      GlNative / GL
```

`AbstractGraphicsPanel` define a API comum. `GraphicsGlPanel` e a implementacao concreta com OpenGL nativo. O usuario normalmente interage com `GraphicsGlPanel`, `GraphicsGlRender`, `GraphicsGlContext`, `GraphicsInput`, `RenderThreadingMode` e `GL`.

## Classes do pacote base

| Classe/interface | Visibilidade | Responsabilidade |
|---|---:|---|
| `AbstractGraphicsPanel<C>` | Publica | Base Swing para paineis graficos. Define renderer, input, FPS, VSync, modo de threading e ciclo de vida. |
| `GraphicsRender<C>` | Publica | Contrato do renderer com callbacks `initialize`, `render`, `resize` e `dispose`. |
| `GraphicsContext` | Publica | Contexto entregue ao renderer. Expoe tamanho, input e `runOnUiThread`. |
| `GraphicsInput` | Publica | Interface somente leitura do estado de teclado, mouse e scroll. |
| `GraphicsInputState` | Publica | Implementacao mutavel de `GraphicsInput`, ligada a listeners Swing/AWT. |
| `GraphicsHost<C>` | Publica | Ponte interna entre um painel e uma superficie grafica concreta; guarda o renderer configurado. |
| `RenderThreadingMode` | Publica | Escolha entre loop compartilhado (`SHARED`) e loop exclusivo (`INDIVIDUAL`). |

## Classes da implementacao OpenGL

| Classe/interface | Visibilidade | Responsabilidade |
|---|---:|---|
| `GraphicsGlPanel` | Publica | Painel Swing concreto. Compoe um `Canvas` AWT, encaminha eventos e registra o host no scheduler. |
| `GraphicsGlRender` | Publica | Especializacao de `GraphicsRender<GraphicsGlContext>` com helpers `runOnUi` e `runOnUiThread`. |
| `GraphicsGlContext` | Publica | Contexto entregue ao renderer GL. Expoe tamanho, input, FPS medido, delta time e frame count. |
| `GL` | Publica | Bindings Java para funcoes OpenGL usadas pelo projeto. Carrega a biblioteca nativa. |
| `GraphicsGlHost` | Interna ao pacote | Dono real do contexto nativo, fila de tarefas, renderer inicializado e estado de renderizacao. |
| `GraphicsGlCanvas` | Interna ao pacote | `Canvas` AWT usado como superficie nativa do OpenGL. |
| `GraphicsGlUiSchedule` | Interna ao pacote | Scheduler que executa hosts em uma thread compartilhada ou em threads individuais. |
| `GraphicsGlRenderContextRegistry` | Interna ao pacote | Associa um `GraphicsGlRender` ao seu contexto para permitir `renderer.runOnUiThread(...)`. |
| `GlNative` | Interna ao pacote | Metodos JNI de contexto: criar, tornar atual, swap, VSync e destruir. |
| `GlNativeLoader` | Interna ao pacote | Resolve e carrega `graphicsgl` e dependencias a partir de `src/main/resources/native`. |

## Ciclo de vida

O ciclo normal de um `GraphicsGlPanel` e:

1. O painel e criado na EDT.
2. O renderer e definido no construtor ou por `setRenderer(...)`.
3. O painel entra na hierarquia Swing.
4. `addNotify()` registra o `GraphicsGlHost` no `GraphicsGlUiSchedule`.
5. No primeiro frame valido, o host cria o contexto nativo com `GlNative.nCreateContext(canvas)`.
6. O contexto vira atual na thread de render com `GlNative.nMakeCurrent(...)`.
7. O renderer atual recebe `initialize(context)` uma vez.
8. O renderer recebe `resize(context, width, height)` apos inicializar e sempre que o canvas muda de tamanho.
9. A cada frame, o contexto atualiza `deltaTime`, `frameCount` e `fps`; depois chama `render(context)` e faz `swapBuffers`.
10. Ao trocar renderer ou descartar o painel, o renderer anterior recebe `dispose(context)`.
11. Ao encerrar, o host destroi o contexto nativo e limpa filas/estado.

`getRenderer()` pode ser usado para recuperar a instancia configurada no painel/host. Ele nao indica, por si so, que o renderer ja passou por `initialize(context)`.

## Threading

Existem duas threads importantes:

| Thread | Responsabilidade |
|---|---|
| EDT do Swing | Criar componentes Swing, adicionar/remover da tela e processar listeners de UI. |
| Thread de render GL | Criar contexto, executar callbacks do renderer e chamar funcoes `GL.*`. |

Chamadas OpenGL devem acontecer na thread em que o contexto esta atual. Por isso, listeners Swing, timers, workers e `CompletableFuture` nao devem chamar `GL.*` diretamente.

O recomendado e concentrar chamadas `GL.*` dentro do renderer, nos callbacks `initialize(...)`, `render(...)`, `resize(...)` e `dispose(...)`. Multithread e valido para preparar dados, calcular malhas, carregar arquivos ou montar arrays fora do renderer; a acao de desenhar na tela e atualizar recursos GL deve voltar para o renderer.

Padrao recomendado: um worker produz dados e entrega ao renderer por uma estrutura thread-safe. No frame seguinte, o proprio `render(...)` consome os dados e faz o upload/desenho:

```java
class MeshRender implements GraphicsGlRender {
    private final java.util.concurrent.atomic.AtomicReference<float[]> pendingVertices =
            new java.util.concurrent.atomic.AtomicReference<>();
    private int vbo;

    void submitVertices(float[] vertices) {
        pendingVertices.set(vertices);
    }

    @Override
    public void render(GraphicsGlContext context) {
        float[] vertices = pendingVertices.getAndSet(null);
        if (vertices != null) {
            GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
            GL.glBufferData(GL.GL_ARRAY_BUFFER, vertices, GL.GL_DYNAMIC_DRAW);
        }

        GL.glClear(GL.GL_COLOR_BUFFER_BIT);
        GL.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
    }
}
```

`panel.runOnUiThread(...)` existe como recurso de escape para casos em que algum codigo externo saiu do renderer e, por algum motivo inevitavel, precisa executar uma tarefa curta no contexto GL. Ele deve ser tratado como contorno raro, nao como forma de desenhar fora do `render(...)`:

```java
panel.runOnUiThread(() -> {
    GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
    GL.glBufferData(GL.GL_ARRAY_BUFFER, vertices, GL.GL_DYNAMIC_DRAW);
});
```

Quando o agendamento precisar existir, prefira que ele parta do proprio renderer e seja usado para uma tarefa pontual de recurso, nao para renderizacao de frame:

```java
runOnUiThread(() -> uploadMesh(vertices));
```

Mesmo nesse caso, use com criterio. O helper funciona depois que o renderer foi associado a um `GraphicsGlContext`. Antes de `initialize(...)`, chamar o helper pode gerar `IllegalStateException`.

## RenderThreadingMode

| Modo | Como funciona | Quando usar |
|---|---|---|
| `SHARED` | Varios hosts rodam em uma unica thread `SwingTools-GL-Render`. | Padrao. Bom para poucos paineis, previews, dashboards e render leve. |
| `INDIVIDUAL` | Cada painel recebe uma thread `SwingTools-GL-Render-N`. | Use quando um painel tem carga pesada, latencia sensivel ou nao deve disputar loop com outros paineis. |

`setRenderMode(...)` precisa ser chamado antes de o painel comecar a renderizar. A base rejeita troca durante `isRendering()`.

## FPS e VSync

`setFPS(int)` define o alvo do scheduler quando VSync esta desligado. Se o valor for menor ou igual a zero, o host agenda o proximo frame imediatamente.

`setVsync(true)` pede ao nativo para sincronizar `swapBuffers` com o monitor. Quando VSync esta ligado, o controle real de cadencia fica no driver/sistema, e o alvo de FPS do scheduler deixa de ser a principal limitacao.

Use `getCurrentFPS()` para mostrar o FPS medido pelo contexto, nao o alvo configurado.

## Input

`GraphicsInputState` acumula:

| Metodo | Significado |
|---|---|
| `isKeyDown(int keyCode)` | Tecla AWT pressionada no momento. |
| `isAnyKeyDown()` | Existe alguma tecla pressionada. |
| `isMouseButtonDown(int button)` | Botao AWT pressionado no momento. |
| `getMouseX()` / `getMouseY()` | Posicao mais recente do mouse na superficie. |
| `isMouseInside()` | Mouse esta dentro do canvas. |
| `getWheelRotation()` | Soma acumulada da rolagem precisa do wheel. |

Ao perder foco, teclas e botoes pressionados sao limpos. A rolagem e acumulada; se o renderer usar wheel como zoom absoluto, converta esse valor para escala. Se precisar de delta por frame, guarde o valor anterior no renderer e subtraia.

## Exemplo minimo

```java
GraphicsGlPanel panel = new GraphicsGlPanel(new GraphicsGlRender() {
    @Override
    public void initialize(GraphicsGlContext context) {
        System.out.println(GL.glGetString(GL.GL_VERSION));
    }

    @Override
    public void resize(GraphicsGlContext context, int width, int height) {
        GL.glViewport(0, 0, width, height);
    }

    @Override
    public void render(GraphicsGlContext context) {
        GL.glClearColor(0.08f, 0.09f, 0.12f, 1f);
        GL.glClear(GL.GL_COLOR_BUFFER_BIT);
    }
});

panel.setFPS(60);
panel.setVsync(true);
```

## Recursos nativos

`GL` e `GlNative` chamam `GlNativeLoader.load()`. O loader tenta carregar:

| Plataforma | Recurso esperado |
|---|---|
| Windows | `/native/win/{os.arch}/graphicsgl.dll` e `libwinpthread-1.dll` |
| Linux | `/native/linux/{os.arch}/libgraphicsgl.so` |
| macOS | Ainda nao suportado pelo loader do `GraphicsGlPanel` |

Se o recurso empacotado nao existir, o loader tenta `/native/{arquivo}` e depois `System.loadLibrary("graphicsgl")`.

## Boas praticas

- Crie e adicione `GraphicsGlPanel` na EDT.
- Defina `setFPS`, `setVsync` e `setRenderMode` antes de mostrar o painel.
- Use `getRenderer()` para consultar a instancia configurada; mantenha chamadas OpenGL dentro dos callbacks do renderer.
- Crie recursos GL em `initialize(...)`; delete em `dispose(...)`.
- Atualize viewport, camera e buffers dependentes de tamanho em `resize(...)`.
- Chame `requestFocusInWindow()` se o renderer depender de teclado.
- Prefira manter chamadas `GL.*` dentro do renderer.
- Use workers para preparar dados, nao para desenhar.
- Use `runOnUiThread(...)` apenas como recurso de escape quando algum codigo externo saiu do renderer e realmente precisar executar uma tarefa curta no contexto GL.
- Ao usar `DISPOSE_ON_CLOSE`, adicione um `WindowListener` que chama `panel.dispose()` se precisar liberar o contexto imediatamente.
- Para tarefas paralelas, gere dados em workers e deixe o renderer consumir esses dados e fazer o upload GL no frame seguinte; nao use `panel.runOnUiThread(...)` como caminho normal para desenhar na tela.

## Exemplos

| Exemplo | Demonstra |
|---|---|
| `GraphicsGlPanelExample` | Triangulo OpenGL, input por frame, VSync, FPS e atualizacao segura por botao Swing. |
| `GraphicsGlCubeExample` | Cubo 3D, depth test, matriz MVP, mouse orbital e zoom por wheel. |
| `GraphicsGlParallelRunOnUiExample` | Geracao paralela de malha e uso excepcional de `runOnUiThread` para upload no contexto GL. |

Veja tambem:

- [AbstractGraphicsPanel.md](AbstractGraphicsPanel.md)
- [GraphicsGlPanel.md](GraphicsGlPanel.md)
