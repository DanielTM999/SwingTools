# ModernComponentDialog

`ModernComponentDialog<T>` cria um dialog moderno com qualquer `JComponent` como conteudo e retorno tipado.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.popup` |
| API | `ModernComponentDialog.modernDialogBuilder(Tipo.class)` |
| Retorno | `T` ou `null` quando cancelado/fechado |

## Componente customizado

```java
JPanel panel = new JPanel();
JTextField nome = new JTextField();
panel.add(nome);

Usuario usuario = ModernComponentDialog.modernDialogBuilder(Usuario.class)
        .title("Usuario")
        .message("Informe os dados.")
        .type(ModernDialog.Type.QUESTION)
        .component(panel)
        .result(ctx -> new Usuario(nome.getText()))
        .onValidate(ctx -> {
            if (nome.getText().isBlank()) {
                throw new IllegalArgumentException("Informe o nome.");
            }
        })
        .parent(frame)
        .show();
```

## Formulario interno

```java
Map<String, Object> values = ModernComponentDialog.modernDialogBuilder(Map.class)
        .title("Cadastro")
        .form(form -> form
                .field("nome", "Nome", new JTextField())
                .field("ativo", "Ativo", new JCheckBox()))
        .parent(frame)
        .show();
```

## Fachada Dialogs

```java
Usuario usuario = Dialogs.componentBuilder(Usuario.class)
        .parent(frame)
        .title("Usuario")
        .form(form -> form.field("nome", "Nome", new JTextField()))
        .result(ctx -> new Usuario(ctx.form().text("nome")))
        .show();
```

## Builder

| Metodo | Uso |
|---|---|
| `title(String)` / `message(String)` | Textos |
| `type(ModernDialog.Type)` | Tipo visual (`INFO`, `SUCCESS`, `ERROR`, `QUESTION`) |
| `accentColor(Color)` | Cor de destaque |
| `component(JComponent)` / `content(JComponent)` | Conteudo customizado |
| `parent(Component)` / `parentComponent(Component)` | Janela/componente de referencia para centralizacao |
| `form(FormConfigurer)` | Cria `FormPanel` interno |
| `result(ResultProvider<T>)` | Monta o valor retornado |
| `onValidate(ValidationHandler<T>)` | Valida antes de confirmar |
| `onSubmit(SubmitHandler<T>)` | Executa acao apos gerar o resultado |
| `option(String, T)` | Botao que retorna valor fixo |
| `submitOption(String)` | Botao que valida e retorna `result(...)` |
| `submitOption(String, ResultProvider<T>)` | Botao com retorno dinamico proprio |
| `cancelOption(String)` | Botao que fecha retornando `null` |
| `confirmText(String)` / `cancelText(String)` | Labels dos botoes padrao |
| `show()` / `show(Component)` | Exibe e retorna `T` |

## Retorno padrao

Se `result(...)` nao for informado, o dialog tenta retornar valores simples:

- `Map.class` quando o conteudo for `FormPanel`.
- `String.class` quando o conteudo for `JTextComponent`.
- item selecionado quando o conteudo for `JComboBox`.
- `Boolean.class` quando o conteudo for `JCheckBox`.
- o proprio componente quando ele for instancia do tipo informado.
