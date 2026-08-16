# NumberField

`NumberField` e um campo numerico baseado em `BigDecimal`, com formatacao por
locale, precisao, intervalo e passo configuraveis.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.textfield` |
| Heranca | `NumberField extends JTextFieldListener` |
| Valor | `BigDecimal` ou `null` quando vazio |

## Uso

```java
NumberField quantidade = new NumberField(Locale.forLanguageTag("pt-BR"))
        .setDecimalPlaces(2)
        .setRange(BigDecimal.ZERO, new BigDecimal("1000"))
        .setStep(new BigDecimal("0.25"))
        .setValue(new BigDecimal("10.50"), false);
```

O locale define o separador decimal mostrado e aceito. Separadores de milhares
nao sao usados durante a edicao.

## Configuracao

| Metodo | Contrato |
|---|---|
| `getValue()` | Retorna o valor atual, inclusive antes da confirmacao |
| `setValue(value)` | Define, normaliza e dispara `CHANGE` quando necessario |
| `setValue(value, fireEvent)` | Define controlando o evento |
| `setRange(min, max)` | Define limites opcionais; `null` remove um limite |
| `setMinimumValue(min)` | Define somente o minimo |
| `setMaximumValue(max)` | Define somente o maximo |
| `setStep(step)` | Define incremento positivo |
| `setDecimalPlaces(places)` | Define casas decimais; zero cria comportamento inteiro |
| `setRoundingMode(mode)` | Define o arredondamento, por padrao `HALF_UP` |
| `setNumberLocale(locale)` | Troca locale e reformata o valor |
| `commitValue()` | Confirma e normaliza o texto programaticamente |
| `isValueWithinRange()` | Informa se o valor editado esta dentro dos limites |

Os padroes sao duas casas decimais, passo `1`, nenhum limite, locale da JVM e
campo vazio permitido.

## Interacao e normalizacao

- Seta para cima incrementa e seta para baixo decrementa pelo passo.
- A roda do mouse altera o valor apenas quando o campo possui foco.
- Enter ou perda de foco arredonda e limita o valor ao intervalo configurado.
- Estados intermediarios como vazio, `-` ou apenas o separador decimal sao
  permitidos durante a digitacao e viram vazio na confirmacao.
- Texto nao numerico, agrupamento e casas alem da precisao configurada sao
  rejeitados pelo filtro do documento.

## Eventos

| Evento | Quando | Valor |
|---|---|---|
| `EventType.INPUT` | Edicao aceita ou incremento | `BigDecimal` ou `null` |
| `EventType.CHANGE` | Valor confirmado mudou | `BigDecimal` ou `null` |
| `EventType.SUBMIT` | Confirmacao por Enter | Valor ja normalizado |

```java
quantidade.addEventListener(EventType.CHANGE, event -> {
    BigDecimal value = event.tryGetValue();
});
```

O exemplo executavel
`src/test/java/dtm/stools/examples/NumberFieldExample.java` demonstra campos
inteiros e decimais, locales `pt-BR` e `en-US`, valor vazio, limites, passo e o
log dos eventos `INPUT`, `CHANGE` e `SUBMIT`.
