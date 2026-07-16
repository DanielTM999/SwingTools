# GraphicsGlPanel

`GraphicsGlPanel` e um painel Swing para renderizacao OpenGL usando o contexto nativo do SwingTools. Ele estende `AbstractGraphicsPanel<GraphicsGlContext>` e usa `GraphicsGlRender` para organizar inicializacao, desenho, resize e descarte de recursos GL.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.graphics.gl` |
| Heranca | `GraphicsGlPanel extends AbstractGraphicsPanel<GraphicsGlContext>` |
| Renderer | `GraphicsGlRender` ou `GraphicsRender<GraphicsGlContext>` |
| Contexto | `GraphicsGlContext` |
| Helper GL | `GL` |

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

## API

| Metodo | Uso |
|---|---|
| `setRenderer(GraphicsRender<GraphicsGlContext>)` | Troca o renderer GL |
| `getGraphicsContext()` | Retorna o contexto GL do painel |
| `runOnUiThread(Runnable)` | Executa trabalho na thread do contexto GL |
| `getInput()` | Retorna estado de teclado, mouse e scroll |
| `setFPS(int)` / `getFPS()` | Define e le FPS alvo |
| `getCurrentFPS()` | FPS medido pelo contexto |
| `setVsync(boolean)` / `isVsync()` | Controla VSync |
| `setRenderMode(RenderThreadingMode)` | Escolhe scheduler compartilhado ou individual antes de renderizar |
| `init()` | Registra o painel no scheduler se ele estiver displayable |
| `dispose()` | Encerra renderizacao e libera contexto/renderer |

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

`GraphicsGlRender` tambem oferece `runOnUi(...)` e `runOnUiThread(...)`, uteis quando um worker precisa atualizar buffers ou estado que pertence ao contexto GL.

## GraphicsGlContext

| Metodo | Uso |
|---|---|
| `getWidth()` / `getHeight()` | Tamanho atual do contexto |
| `getDeltaTime()` | Tempo em segundos desde o frame anterior |
| `getFps()` | FPS medido |
| `getFrameCount()` | Total de frames renderizados |
| `getInput()` | Estado de input |
| `runOnUiThread(Runnable)` | Agenda trabalho no contexto |

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

## Recursos nativos

`GraphicsGlPanel` depende do binario nativo `graphicsgl` empacotado em `src/main/resources/native`.

| Plataforma | Binario |
|---|---|
| Windows | `src/main/resources/native/win/amd64/graphicsgl.dll` |

O codigo-fonte nativo fica em `native/win/GraphicsGl.cpp` e `native/linux/GraphicsGl.cpp`.

## Exemplos

| Classe | Demonstra |
|---|---|
| `GraphicsGlPanelExample` | Triangulo OpenGL, input, VSync e FPS |
| `GraphicsGlCubeExample` | Cubo 3D com recursos GL |
| `GraphicsGlParallelRunOnUiExample` | Geracao paralela e atualizacao segura via `runOnUiThread` |

## Cuidados

- Chame `dispose()` ao fechar janelas com `DISPOSE_ON_CLOSE` quando precisar liberar o contexto imediatamente.
- Crie objetos GL em `initialize(...)` e delete-os em `dispose(...)`.
- Use `runOnUiThread(...)` para tocar em recursos GL fora do callback `render(...)`.
- Chame `requestFocusInWindow()` se o renderer depender de teclado.
