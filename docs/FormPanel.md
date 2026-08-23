# FormPanel, FormField e validacao

O pacote `dtm.stools.component.form` traz a infraestrutura de formulario: um container que organiza campos em colunas, um wrapper visual por campo e um conjunto de regras de validacao.

| Classe | Papel |
|---|---|
| `FormPanel` | Container: colunas, valores, validacao em bloco e submit |
| `FormField` | Rotulo, controle, texto de ajuda e mensagem de erro |
| `Validator<T>` | Regra de validacao encadeavel |
| `ValidationResult` | Resultado da validacao (`valid` + `message`) |
| `Validators` | Regras prontas |
| `FormValues` | Leitura e escrita de valor por tipo de controle |

## Montagem

```java
FormPanel form = new FormPanel(2);

form.addField(new FormField("nome", "Nome", new JTextField())
        .setRequired(true)
        .setHelperText("Nome completo"));

form.addField("email", "E-mail", new JTextField(),
        Validators.<String>required().and(Validators.email()));

form.addField("cpf", "CPF", new JTextField(), Validators.cpf());
form.addField(new FormField("termos", "Termos", new CheckBoxField("Aceito")).setRequired(true));

form.addEventListener(EventType.SUBMIT, event -> {
    Map<String, Object> valores = event.tryGetValue();
});
```

## FormPanel

| Metodo | Contrato |
|---|---|
| `addField(String name, String label, JComponent control)` | Adiciona um campo |
| `addField(String name, String label, JComponent control, Validator<T>)` | Adiciona com regra |
| `addField(FormField)` | Adiciona um campo ja construido; nome duplicado lanca `IllegalArgumentException` |
| `addSectionTitle(String)` | Titulo de secao ocupando a linha inteira |
| `getField(String)` / `getFields()` | Acesso aos campos |
| `getValues()` | Mapa nome para valor, na ordem de insercao |
| `setValues(Map<String,Object>)` | Escreve nos campos correspondentes, sem disparar eventos |
| `validateAll()` | Valida tudo e devolve o mapa de erros |
| `isFormValid()` | Atalho para `validateAll().isEmpty()` |
| `submit()` | Valida e, sem erros, dispara `EventType.SUBMIT` com os valores |
| `reset()` | Zera os campos e dispara `EventType.CLEAR` |
| `clearErrors()` | Limpa as mensagens sem mexer nos valores |
| `setColumns(int)` / `getColumns()` | Quantidade de colunas |
| `setGap(int)` | Espacamento entre campos |

Eventos do painel: `FormPanel.VALIDATION_PASSED`, `FormPanel.VALIDATION_FAILED`, `EventType.VALIDATE`, `EventType.SUBMIT` e `EventType.CLEAR`.

> O metodo de validacao chama-se `isFormValid()`, e nao `isValid()`, porque `isValid()` pertence a `java.awt.Component` e e chamado pelo Swing a cada invalidacao de layout.

## FormField

| Metodo | Contrato |
|---|---|
| `getFieldName()` | Chave do campo dentro do formulario |
| `getControl()` | Controle envolvido |
| `getValue()` / `setValue(Object)` | Le e escreve via `FormValues` |
| `setValidator(Validator<T>)` | Regra aplicada ao valor |
| `setRequired(boolean)` / `isRequired()` | Marca com `*` e encadeia `Validators.required()` |
| `setHelperText(String)` | Texto de ajuda exibido quando nao ha erro |
| `setLabelText(String)` | Rotulo |
| `setValidateOnChange(boolean)` | Revalida a cada alteracao apos a primeira falha |
| `validateField()` | Executa a validacao e atualiza a mensagem |
| `isFieldValid()` | Resultado da ultima validacao |
| `setError(String)` / `clearError()` | Controle manual do estado de erro |
| `reset()` | Zera o controle e limpa o erro |

Em erro, o campo pinta uma barra de acento a esquerda do controle, marca a mensagem em vermelho e aplica `JComponent.outline = "error"` ao controle. Para um `TextAreaField`, usa `setErrorState(true)`.

Eventos do campo: `FormField.VALID`, `FormField.INVALID` e `EventType.VALIDATE`.

O construtor tambem chama `setName(name)`, entao o campo continua localizavel por `findById`.

## Validators

| Regra | Uso |
|---|---|
| `required()` / `required(String)` | Nao nulo e nao vazio |
| `minLength(int)` / `maxLength(int)` | Comprimento de texto |
| `pattern(String regex, String message)` | Expressao regular |
| `email()` | Endereco de e-mail |
| `range(BigDecimal min, BigDecimal max)` | Intervalo numerico |
| `cpf()` / `cnpj()` | Documentos brasileiros, com digito verificador |
| `matches(Supplier<T> other, String message)` | Igualdade com outro valor, util em confirmacao de senha |
| `of(Predicate<T>, String message)` | Regra a partir de um predicado |

`required()` considera vazio: `null`, texto em branco, colecao vazia, array vazio e `Boolean.FALSE` — este ultimo faz um checkbox obrigatorio exigir marcacao.

Regras encadeiam com `and`, e a primeira falha interrompe a cadeia:

```java
Validator<String> regra = Validators.<String>required()
        .and(Validators.minLength(8))
        .and(Validators.pattern(".*\\d.*", "Precisa de um numero"));
```

As mensagens padrao passam por `I18n.getText(Validators.class, ...)`, com chaves sob o prefixo `Validators.`.

## FormValues

`FormValues.read`, `write` e `clear` conhecem `JTextComponent`, `JComboBox`, `AbstractButton`, `JSpinner`, `JSlider`, `JList` e os componentes do SwingTools: `CheckBoxField`, `SwitchField`, `RadioGroupField`, `SegmentedField`, `SliderField`, `RatingField`, `PinField`, `StepperField`, `TextAreaField` e `DualListField`.

Para um controle nao suportado, `read` devolve `null` e `write` nao faz nada; nesse caso, envolva o controle em uma subclasse de `FormField` que sobrescreva `getValue` e `setValue`.
