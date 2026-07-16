# GraphicsGlPanel

`GraphicsGlPanel` e um painel Swing para renderizacao OpenGL usando o contexto nativo do SwingTools. Ele estende `AbstractGraphicsPanel<GraphicsGlContext>`, coloca um `Canvas` AWT dentro de um `JPanel` e usa `GraphicsGlRender` para organizar inicializacao, desenho, resize e descarte de recursos GL.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.graphics.gl` |
| Heranca | `GraphicsGlPanel extends AbstractGraphicsPanel<GraphicsGlContext>` |
| Renderer | `GraphicsGlRender` ou `GraphicsRender<GraphicsGlContext>` |
| Contexto | `GraphicsGlContext` |
| Helper GL | `GL` |
| Superficie nativa | `GraphicsGlCanvas extends Canvas` |

## Arquitetura interna

```text
GraphicsGlPanel
  GraphicsGlHost
    GraphicsGlCanvas
    GraphicsGlContext
    GraphicsInputState
    Queue<Runnable> uiTasks
    GraphicsRender<GraphicsGlContext>
  GraphicsGlUiSchedule
  GlNative / GL
```

O `GraphicsGlPanel` e a parte Swing publica. O `GraphicsGlHost` e o dono real do contexto nativo, da fila de tarefas GL, do renderer inicializado e dos flags de estado. O `GraphicsGlCanvas` e a superficie AWT que o nativo usa para criar o contexto.

## Criacao

```java
GraphicsGlPanel panel = new GraphicsGlPanel(new TriangleRender());
panel.setFPS(60);
panel.setVsync(true);

JFrame frame = new JFrame("OpenGL");
frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
frame.add(panel, BorderLayout.CENTER);
frame.setSize(800, 600);
frame.setLocationRelativeTo(null);
frame.setVisible(true);
panel.requestFocusInWindow();
```

Tambem e possivel criar vazio e definir o renderer depois:

```java
GraphicsGlPanel panel = new GraphicsGlPanel();
panel.setRenderer(new TriangleRender());
```

`addNotify()` registra automaticamente o host no scheduler quando o painel entra na hierarquia Swing. `init()` existe para registro manual se o componente ja estiver displayable.

## API

| Metodo | Uso |
|---|---|
| `setRenderer(GraphicsRender<GraphicsGlContext>)` | Troca o renderer GL |
| `getRenderer()` | Retorna o renderer GL atualmente configurado |
| `getGraphicsContext()` | Retorna o contexto GL do painel |
| `runOnUiThread(Runnable)` | Executa trabalho na thread do contexto GL |
| `getInput()` | Retorna estado de teclado, mouse e scroll |
| `setFPS(int)` / `getFPS()` | Define e le FPS alvo |
| `getCurrentFPS()` | FPS medido pelo contexto |
| `setVsync(boolean)` / `isVsync()` | Controla VSync |
| `setRenderMode(RenderThreadingMode)` | Escolhe scheduler compartilhado ou individual antes de renderizar |
| `init()` | Registra o painel no scheduler se ele estiver displayable |
| `dispose()` | Encerra renderizacao e libera contexto/renderer |

## Ciclo de render

O host executa o seguinte fluxo:

1. Se o canvas ainda nao esta displayable, agenda outro frame.
2. Se nao existe contexto, chama `GlNative.nCreateContext(canvas)`.
3. Torna o contexto atual com `GlNative.nMakeCurrent(handle)`.
4. Aplica VSync quando necessario.
5. Executa tarefas pendentes da fila de `runOnUiThread(...)`.
6. Se o renderer mudou, descarta o renderer anterior e inicializa o novo.
7. Atualiza tamanho do `GraphicsGlContext`.
8. Chama `resize(...)` quando o tamanho muda.
9. Atualiza timing (`deltaTime`, `fps`, `frameCount`).
10. Chama `render(context)`.
11. Chama `GlNative.nSwapBuffers(handle)`.
12. Agenda o proximo frame conforme FPS/VSync.

Se `setRenderer(...)` for chamado com outro renderer, o renderer antigo recebe `dispose(context)` antes de o novo receber `initialize(context)`. `getRenderer()` retorna a instancia configurada no host, mas ela pode ainda nao estar inicializada se o contexto GL nao tiver sido criado.

## GraphicsGlRender

```java
class TriangleRender implements GraphicsGlRender {
    private int program;
    private int vao;
    private int vbo;

    @Override
    public void initialize(GraphicsGlContext context) {
        program = GL.glCreateProgram();
        vao = GL.glGenVertexArrays();
        vbo = GL.glGenBuffers();
        // Compile shaders, envie buffers e configure atributos.
    }

    @Override
    public void render(GraphicsGlContext context) {
        GL.glClearColor(0.1f, 0.1f, 0.14f, 1f);
        GL.glClear(GL.GL_COLOR_BUFFER_BIT);
        GL.glUseProgram(program);
        GL.glBindVertexArray(vao);
        GL.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
    }

    @Override
    public void resize(GraphicsGlContext context, int width, int height) {
        GL.glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(GraphicsGlContext context) {
        if (program != 0) GL.glDeleteProgram(program);
        if (vbo != 0) GL.glDeleteBuffers(vbo);
        if (vao != 0) GL.glDeleteVertexArrays(vao);
    }
}
```

`GraphicsGlRender` tambem oferece `runOnUi(...)` e `runOnUiThread(...)`. Esses helpers sao recursos de escape para tarefas pontuais no contexto GL. O fluxo recomendado continua sendo manter chamadas `GL.*` e qualquer acao de desenho dentro do renderer.

Esses helpers dependem de `GraphicsGlRenderContextRegistry`. O renderer so entra no registry enquanto esta inicializado. Chamar `runOnUiThread(...)` antes de o renderer estar ligado ao contexto gera `IllegalStateException`.

## GraphicsGlContext

| Metodo | Uso |
|---|---|
| `getWidth()` / `getHeight()` | Tamanho atual do contexto |
| `getDeltaTime()` | Tempo em segundos desde o frame anterior |
| `getFps()` | FPS medido |
| `getFrameCount()` | Total de frames renderizados |
| `getInput()` | Estado de input |
| `runOnUiThread(Runnable)` | Agenda trabalho no contexto |

`getDeltaTime()` vem da diferenca entre o frame atual e o anterior, em segundos. O primeiro frame apos reset tem `deltaTime` zero. `getFps()` e calculado em janelas aproximadas de um segundo. `getFrameCount()` cresce a cada frame renderizado.

## Input

O painel encaminha eventos de teclado e mouse da superficie nativa para o componente Swing, entao listeners Swing no `GraphicsGlPanel` continuam funcionando. Para input por frame, use `context.getInput()`:

```java
GraphicsInput input = context.getInput();
if (input.isKeyDown(KeyEvent.VK_LEFT)) {
    angle -= 2f * context.getDeltaTime();
}
if (input.isMouseButtonDown(MouseEvent.BUTTON1) && input.isMouseInside()) {
    float x = input.getMouseX() / (float) context.getWidth();
}
```

Detalhes importantes:

- `requestFocusInWindow()` e encaminhado para o canvas interno.
- O canvas pede foco ao receber `mousePressed`.
- Ao perder foco, teclas e botoes pressionados sao limpos.
- `getWheelRotation()` e acumulado, nao zerado por frame.

## Threading e runOnUiThread

No `GraphicsGlPanel`, `runOnUiThread(...)` agenda uma tarefa na thread de render, nao na EDT do Swing. Essa thread e a unica onde o contexto OpenGL esta atual durante o loop.

O uso recomendado e: listeners Swing e workers alteram estado ou entregam dados ao renderer; o renderer, em `render(...)` ou outro callback proprio, faz as chamadas `GL.*`. Multithread pode preparar dados, mas a acao de desenhar na tela continua sendo responsabilidade do `render(...)`.

Use `panel.runOnUiThread(...)` como contorno quando uma acao fora do renderer realmente precisar tocar em recursos GL. Isso normalmente indica que o codigo saiu do renderer e esta tentando executar uma operacao de contexto; nao use esse caminho para desenhar frames:

```java
button.addActionListener(e -> panel.runOnUiThread(() -> {
    GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
    GL.glBufferData(GL.GL_ARRAY_BUFFER, vertices, GL.GL_DYNAMIC_DRAW);
}));
```

Se `runOnUiThread(...)` for chamado de dentro da propria thread de render, a tarefa roda imediatamente. Caso contrario, ela entra em uma fila concorrente e o host acorda a thread de render. Essa fila existe para integrar casos excepcionais; nao use como substituta do desenho normal dentro do renderer.

Padrao preferido para trabalho paralelo:

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

        GL.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
    }
}
```

## Scheduler

`GraphicsGlUiSchedule` suporta dois modos:

| Modo | Thread | Uso |
|---|---|---|
| `RenderThreadingMode.SHARED` | Uma thread `SwingTools-GL-Render` para varios hosts | Padrao, economiza threads e funciona bem para render leve/moderado. |
| `RenderThreadingMode.INDIVIDUAL` | Uma thread `SwingTools-GL-Render-N` por painel | Melhor para render pesado ou painel que nao deve disputar loop com outros. |

Configure antes de o painel renderizar:

```java
panel.setRenderMode(RenderThreadingMode.INDIVIDUAL);
```

Depois que `isRendering()` fica `true`, `setRenderMode(...)` lanca `IllegalStateException`.

## FPS e VSync

`setFPS(int)` altera o alvo usado pelo scheduler quando VSync esta desligado. O padrao do host e 60 FPS.

`setVsync(boolean)` marca o estado como "dirty" e acorda a thread de render. No frame seguinte, o host chama `GlNative.nSetVsync(contextHandle, vsync)`.

Quando VSync esta ligado, o tempo de `swapBuffers` pode ser controlado pelo driver/sistema. Nesse caso, `getCurrentFPS()` reflete o resultado real, nao necessariamente o valor de `setFPS(...)`.

## Troca e descarte de renderer

O host guarda `initializedRenderer`. Quando `setRenderer(...)` aponta para outra instancia:

1. `dispose(...)` e chamado no renderer anterior.
2. O renderer anterior e removido do registry.
3. O novo renderer entra no registry se implementar `GraphicsGlRender`.
4. O novo renderer recebe `initialize(...)`.
5. O novo renderer recebe `resize(...)` com o tamanho atual.

Isso permite trocar cenas em runtime, mas os recursos GL sempre devem ser de propriedade do renderer que os criou.

`getRenderer()` consulta o renderer configurado, nao necessariamente o `initializedRenderer` interno. Use esse getter para inspecao ou integracao com codigo que precisa recuperar a instancia; chamadas OpenGL continuam pertencendo aos callbacks do renderer ou a tarefas curtas via `runOnUiThread(...)`.

## Recursos nativos

`GraphicsGlPanel` depende do binario nativo `graphicsgl` empacotado em `src/main/resources/native`.

| Plataforma | Binario |
|---|---|
| Windows | `src/main/resources/native/win/amd64/graphicsgl.dll` |
| Linux | `src/main/resources/native/linux/{arch}/libgraphicsgl.so` quando empacotado |
| macOS | Ainda nao suportado pelo loader GL atual |

O codigo-fonte nativo fica em `native/win/GraphicsGl.cpp` e `native/linux/GraphicsGl.cpp`.

No Windows, o loader tambem tenta carregar `libwinpthread-1.dll` do mesmo diretorio. Antes do `graphicsgl`, ele tenta carregar `jawt` a partir do JDK.

## GL

`GL` e um wrapper JNI com constantes e funcoes OpenGL usadas pelos exemplos e pelo projeto. Ele cobre o conjunto basico para:

| Area | Funcoes |
|---|---|
| Estado e clear | `glClear`, `glClearColor`, `glViewport`, `glEnable`, `glDisable`, `glGetError`, `glGetString` |
| Buffers | `glGenBuffers`, `glBindBuffer`, `glBufferData`, `glBufferDataInt`, `glDeleteBuffers` |
| Vertex arrays | `glGenVertexArrays`, `glBindVertexArray`, `glDeleteVertexArrays` |
| Shaders | `glCreateShader`, `glShaderSource`, `glCompileShader`, `glGetShaderi`, `glGetShaderInfoLog`, `glDeleteShader` |
| Programas | `glCreateProgram`, `glAttachShader`, `glLinkProgram`, `glUseProgram`, `glDeleteProgram` |
| Atributos | `glVertexAttribPointer`, `glEnableVertexAttribArray`, `glDisableVertexAttribArray` |
| Uniforms | `glUniform1f`, `glUniform2f`, `glUniform3f`, `glUniform4f`, `glUniform1i`, `glUniformMatrix4fv` |
| Draw | `glDrawArrays`, `glDrawElements` |

## Exemplos

| Classe | Demonstra |
|---|---|
| `GraphicsGlPanelExample` | Triangulo OpenGL, input, VSync e FPS |
| `GraphicsGlCubeExample` | Cubo 3D com recursos GL |
| `GraphicsGlParallelRunOnUiExample` | Geracao paralela e uso excepcional de `runOnUiThread` para atualizar recurso GL |

## Cuidados

- Chame `dispose()` ao fechar janelas com `DISPOSE_ON_CLOSE` quando precisar liberar o contexto imediatamente.
- Crie objetos GL em `initialize(...)` e delete-os em `dispose(...)`.
- Prefira tocar em recursos GL dentro do renderer, especialmente em `initialize(...)`, `render(...)`, `resize(...)` e `dispose(...)`.
- Use workers para preparar dados, nao para desenhar.
- Use `runOnUiThread(...)` apenas como recurso de escape quando algum codigo externo saiu do renderer e realmente precisar executar uma tarefa curta no contexto GL.
- Chame `requestFocusInWindow()` se o renderer depender de teclado.
- Nao chame `GL.*` diretamente em listeners Swing, timers Swing ou workers.
- Evite trabalho pesado dentro de `render(...)`; gere dados fora e deixe o renderer consumir esses dados e fazer o upload GL no frame seguinte.
- Se houver varios paineis leves, mantenha `SHARED`; se um painel for pesado, considere `INDIVIDUAL`.

Veja tambem [Graphics.md](Graphics.md) para a visao de todos os componentes do pacote.
