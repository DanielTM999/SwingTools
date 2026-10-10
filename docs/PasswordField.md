# PasswordField

Campo independente em `dtm.stools.component.inputfields.passwordfield`, composto por um `JPasswordField` nativo com botão de olho dentro do campo, à direita. O texto reserva espaço para o ícone. Crie e altere os componentes na EDT.

```java
PasswordField senha = new PasswordField().setPlaceholder("Sua senha");
FormPanel form = new FormPanel();
form.addField(new FormField("senha", "Senha", senha).setRequired(true));
form.setValues(Map.of("senha", new char[]{'a', 'b', 'c'}));
```

| API | Contrato |
|---|---|
| `getPassword()` | Retorna um novo `char[]`; o consumidor deve apagar a cópia após usá-la |
| `getPasswordAsString()` | Retorna o conteúdo da senha como `String`; vazio retorna `""` |
| `setPassword(char[], boolean)` | Copia a entrada; `false` suprime eventos da biblioteca |
| `clear(boolean)` | Limpa o conteúdo |
| `setPasswordVisible(boolean, boolean)` | Alterna a exibição, preservando cursor e seleção |
| `setEditable(boolean)` | Controla edição e botão de visibilidade |
| `getPasswordField()` | Permite personalizar o controle Swing nativo e gerenciar foco |
| `setLabel(String)` / `getLabel()` | Define o rótulo flutuante e o nome acessível; `null` equivale a vazio |
| `setFieldLayoutManager(FieldLayoutManager)` / `getFieldLayoutManager()` | Aplica o layout ao editor nativo; `null` restaura o visual convencional |

Para ler a senha como texto:

```java
String valor = senha.getPasswordAsString();
```

O mesmo `MaterialLayout` dos campos de texto também funciona neste componente:

```java
MaterialLayout material = new MaterialLayout();
PasswordField senha = new PasswordField()
        .setLabel("Senha")
        .setFieldLayoutManager(material);
material.setError("Senha inválida");
material.setErrorColor(Color.RED);
```

O botão de mostrar senha fica na área do editor, acima da mensagem de erro. O layout mantém a senha oculta, a seleção, os eventos e a acessibilidade do `JPasswordField`. A mensagem permanece até `material.setError(null)`.

A senha começa oculta. Clique no olho ou use Alt+V com o foco no campo para mostrar/ocultar; o botão também aceita foco e acionamento pelo teclado. `EventType.CHANGE` informa alteração com valor `null` e sem conteúdo nas propriedades. `VISIBILITY_CHANGED` entrega um booleano. O campo mantém a semântica acessível nativa `PASSWORD_TEXT`; o botão tem nome acessível indicando mostrar ou ocultar.

O olho mantém a mesma cor e não desenha borda ao receber foco. Quando desabilitado, usa a cor de controles desabilitados.

`FormValues` lê este componente e `JPasswordField` como `char[]`, antes da regra genérica de texto. Escrever exige `char[]` ou `null`. Validadores podem usar `Validator<char[]>`; obrigatório verifica o comprimento do array. Arrays temporários da validação são apagados após a chamada, inclusive se o validador lançar uma exceção.

Eventos de validação e limpeza do formulário omitem campos de senha. `getValues()` e o evento explícito `SUBMIT` entregam cópias para uso pelo aplicativo:

```java
char[] valor = senha.getPassword();
try {
    // Use o array na operação do aplicativo.
} finally {
    Arrays.fill(valor, '\0');
}
```

O documento Swing usa texto internamente; `char[]` evita a conversão automática do valor público em `String`, mas não garante remoção de todas as cópias internas do Swing. Não armazene senhas em logs ou mapas além do tempo necessário. Não há medidor de força nesta entrega.

Exemplo executável: `dtm.stools.examples.PasswordFieldExample`.
