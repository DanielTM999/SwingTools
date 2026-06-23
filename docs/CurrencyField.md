# CurrencyField

`CurrencyField` e um campo monetario baseado em `BigDecimal` e `NumberFormat`.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.textfield` |
| Heranca | `CurrencyField extends MaskedTextField` |
| Valor | `BigDecimal` |

## Construtores

| Assinatura | Uso |
|---|---|
| `CurrencyField()` | Usa `Locale.getDefault()` |
| `CurrencyField(Locale locale)` | Usa locale especifico |

## API

| Metodo | Contrato |
|---|---|
| `getValue()` | Retorna o valor monetario como `BigDecimal` |
| `setValue(BigDecimal)` | Define valor e reformata texto |
| `setValueLocale(Locale)` | Troca o formatador de moeda |

## Exemplo

```java
CurrencyField amount = new CurrencyField(new Locale("pt", "BR"));
amount.setValue(new BigDecimal("129.90"));

amount.addEventListner(EventType.CHANGE, event -> {
    BigDecimal value = amount.getValue();
    System.out.println(value);
});
```

## Cuidados

- O campo e para entrada e exibicao monetaria; calculos financeiros devem ser feitos fora dele.
- O numero de casas decimais vem do `NumberFormat` do locale usado na criacao.
- Ao trocar locale, confirme se o texto atual precisa ser reformatado com `setValue(getValue())`.
