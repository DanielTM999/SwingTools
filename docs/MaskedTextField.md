# MaskedTextField

`MaskedTextField` e um campo de texto com mascara opcional, placeholder, modo somente leitura e eventos de input/change/submit.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.textfield` |
| Heranca | `MaskedTextField extends JTextFieldListener` |
| Uso principal | CPF, CNPJ, telefone, codigo, placa, campos formatados e texto com placeholder |

## Mascara

Caracteres especiais:

| Char | Aceita | Conversao |
|---|---|---|
| `#` | Digito | nenhuma |
| `U` | Letra | maiuscula |
| `L` | Letra | minuscula |
| `$` | Letra ou digito | maiuscula |
| `@` | Letra ou digito | minuscula |
| `&` | Letra ou digito | nenhuma |
| `?` | Letra | nenhuma |
| `*` | Qualquer caractere | nenhuma |

Caracteres fora dessa lista sao literais da mascara.

### Alternativas de mascara

Use `|` para separar varias mascaras. O campo aplica a **primeira alternativa** (na ordem) compativel com o que foi digitado.

```java
new MaskedTextField("#:#|####"); // aceita o formato #:# OU ate 4 digitos
```

### Obrigatoriedade por caractere

Dentro de uma alternativa cada posicao pode ser marcada:

| Sintaxe | Significado |
|---|---|
| `X` ou `{X}` | Caractere obrigatorio (precisa ser preenchido) |
| `[X]` | Caractere opcional (pode ficar vazio) |
| `X+` | Um ou mais (repete o caractere, sem limite) |

```java
new MaskedTextField("#[#][#][#]"); // exige 1 digito, aceita ate mais 3 opcionais
new MaskedTextField("#+");          // um ou mais digitos, sem limite
new MaskedTextField("U+-#+");       // 1+ letras, '-', 1+ digitos
```

### Separador digitavel

Um literal logo apos um quantificador `+` vira um **separador digitavel**: como o grupo `X+` nao tem tamanho fixo, o usuario digita o separador para encerra-lo e seguir para o proximo grupo.

```java
new MaskedTextField("#+:#+|#+"); // "digitos:digitos" OU apenas digitos
```

No exemplo acima o usuario digita os primeiros digitos, digita `:` e continua no segundo grupo. Enquanto o `:` nao for digitado, o valor ainda e valido como a alternativa `#+`.

O metodo `isComplete()` retorna `true` quando todos os caracteres obrigatorios da alternativa ativa estao preenchidos.

### Escape de literais

As regras de máscara estao sempre ativas. Para usar os caracteres especiais `[ ] { } | + \` como literais, escape com `\`:

```java
new MaskedTextField("\\[##\\]"); // produz o literal [##]
new MaskedTextField("#\\+#");    // produz o literal + entre dois digitos
```

### Dica visual

O campo inicia vazio e a mascara e construida conforme o usuario digita, escolhendo a alternativa compativel. Quando vazio e sem foco:

- Se `setPlaceholder(...)` foi definido, mostra esse texto.
- Caso contrario, mostra o template de cada alternativa unido por `|` (ex: `#:#|####` exibe `_:_|____`).

## Construtores

| Assinatura | Uso |
|---|---|
| `MaskedTextField()` | Campo simples |
| `MaskedTextField(int columns)` | Campo simples com colunas |
| `MaskedTextField(String mask)` | Campo com mascara |
| `MaskedTextField(String mask, int columns)` | Mascara com colunas |
| `MaskedTextField(String mask, char placeholder)` | Mascara com placeholder customizado |
| `MaskedTextField(String mask, char placeholder, int columns)` | Completo |

## API principal

| Metodo | Contrato |
|---|---|
| `getCleanText()` | Retorna apenas o valor digitado, sem literais |
| `setCleanText(String)` | Preenche o campo aplicando a mascara |
| `isComplete()` | Indica se os caracteres obrigatorios da alternativa ativa estao preenchidos |
| `setPlaceholder(String)` | Placeholder visual quando nao ha mascara |
| `setPlaceholderColor(Color)` | Cor do placeholder |
| `setReadonly(boolean)` / `isReadonly()` | Modo somente leitura visual |
| `setFireChangeOnSetText(boolean)` | Controla evento ao chamar `setText` |

## Eventos

| Evento | Quando |
|---|---|
| `EventType.INPUT` | A cada alteracao aceita pelo filtro |
| `EventType.CHANGE` | Ao perder foco se o valor mudou; tambem em `setCleanText` quando muda |
| `EventType.SUBMIT` | Action do campo, normalmente Enter |

## Exemplo

```java
MaskedTextField cpf = new MaskedTextField("###.###.###-##");
cpf.addEventListner(EventType.CHANGE, event -> {
    String clean = event.tryGetValue();
    System.out.println(clean);
});

cpf.setCleanText("12345678901");
```

## Cuidados

- Para persistencia, prefira `getCleanText()`.
- Placeholder textual so e desenhado quando nao ha mascara.
- `setReadonly(true)` difere de `setEnabled(false)`: mantem o campo visualmente legivel.
