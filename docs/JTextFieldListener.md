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
| `addEventListner(type, consumer)` | Registra listener |
| `removeEventListner(type, consumer)` | Remove listener especifico |
| `removeEventListner(type)` | Remove todos do tipo |
| `removeAllListeners()` | Limpa todos |
| `getEventListners()` | Retorna copia dos listeners |

Subclasses podem usar `dispachEvent(...)` para emitir eventos e `registerValidEvents(...)` para limitar eventos aceitos.

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
