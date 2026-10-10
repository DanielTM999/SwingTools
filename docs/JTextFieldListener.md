# JTextFieldListener

`JTextFieldListener` e um `JTextField` com suporte ao contrato de eventos do SwingTools.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.inputfields.textfield` |
| Heranca | `JTextFieldListener extends JTextField implements EventListenerComponent` |
| Uso principal | Base para campos de texto customizados com eventos padronizados |

## Heranca

```text
JTextField
  JTextFieldListener
    MaskedTextField
      CurrencyField
    SearchTextField<T>
      PathSearchTextField
```

## API

| Metodo | Contrato |
|---|---|
| `addEventListener(type, consumer)` | Registra listener |
| `removeEventListner(type, consumer)` | Remove listener especifico |
| `removeEventListner(type)` | Remove todos do tipo |
| `removeAllListeners()` | Limpa todos |
| `getEventListners()` | Retorna copia dos listeners |

Subclasses podem usar `dispachEvent(...)` para emitir eventos e `registerValidEvents(...)` para limitar eventos aceitos.

## Apresentação do campo

`setLabel(String)` define o rótulo e o nome acessível. `setFieldLayoutManager(FieldLayoutManager)` instala uma estratégia visual no próprio campo; `null` restaura a apresentação convencional. Os getters correspondentes permitem consultar as propriedades.

```java
MaskedTextField cpf = new MaskedTextField("###.###.###-##", 20);
cpf.setLabel("CPF");
MaterialLayout material = new MaterialLayout();
cpf.setFieldLayoutManager(material);
material.setError("CPF inválido");
```

O contrato fica em `dtm.stools.component.inputfields.textfield.layout`. Consulte [MaterialLayout](MaterialLayout.md) para configuração, ciclo de vida e criação de outras estratégias. As subclasses herdam a API; máscaras e eventos continuam sob responsabilidade do campo.

## Exemplo de subclasse

```java
public class UppercaseField extends JTextFieldListener {
    public UppercaseField() {
        getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { emit(); }
            public void removeUpdate(DocumentEvent e) { emit(); }
            public void changedUpdate(DocumentEvent e) { emit(); }
            private void emit() {
                dispachEvent(EventType.INPUT, UppercaseField.this::getText);
            }
        });
    }
}
```

## Cuidados

- Para eventos de texto comuns, `MaskedTextField` ja entrega `INPUT`, `CHANGE` e `SUBMIT`.
- Para valor selecionado com sugestoes, use `SearchTextField<T>`.
