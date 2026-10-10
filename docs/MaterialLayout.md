# MaterialLayout

`MaterialLayout` aplica borda contornada, rótulo flutuante animado e mensagem de erro ao próprio campo Swing. O recurso é herdado de `JTextFieldListener` por `MaskedTextField`, `CurrencyField`, `NumberField`, `SearchTextField`, `PathTextField` e `PathSearchTextField`. `PasswordField` oferece a mesma API e aplica o layout ao seu `JPasswordField` nativo.

## Uso

Crie e configure os campos e layouts na EDT.

```java
import dtm.stools.component.inputfields.textfield.MaskedTextField;
import dtm.stools.component.inputfields.textfield.layout.MaterialLayout;
import java.awt.Color;

MaterialLayout material = new MaterialLayout();
MaskedTextField cpf = new MaskedTextField("###.###.###-##", 20);
cpf.setLabel("CPF");
cpf.setFieldLayoutManager(material);
panel.add(cpf);

material.setError("CPF inválido");
material.setErrorColor(Color.RED);
material.setError(null);       // remove a mensagem
material.setErrorColor(null);  // restaura UiTokens.danger()

cpf.setFieldLayoutManager(null); // volta ao visual convencional
cpf.setFieldLayoutManager(material); // reaplica a estratégia
```

O rótulo pertence ao campo. A mensagem e a cor de erro pertencem ao `MaterialLayout`. A aplicação decide quando validar e remover o erro; digitar e limpar o texto mantêm a mensagem.

## API

| Objeto | Método | Contrato |
|---|---|---|
| Campo | `setLabel` / `getLabel` | Rótulo e nome acessível; `null` equivale a vazio |
| Campo | `setFieldLayoutManager` / `getFieldLayoutManager` | Instala ou consulta a estratégia; `null` restaura o visual convencional |
| Campo | `isFieldContentEmpty()` | Vazio semântico; campos com máscara consideram o valor limpo |
| Campo | `getFieldContentBounds()` | Área de digitação, descontando margens, ícone e limpeza |
| Layout | `setError` / `getError` | Mensagem persistente; `null` ou vazio remove e o getter retorna `null` |
| Layout | `setErrorColor` / `getErrorColor` | Cor personalizada; `null` restaura o tema e o getter retorna a cor efetiva |

Os setters do `MaterialLayout` retornam o próprio layout para encadeamento. Cada instância atende um campo por vez: compartilhar uma instância entre campos ativos lança `IllegalStateException`. Após desconectar, é possível reutilizá-la.

## Comportamento

- Campo vazio sem foco: rótulo dentro do campo. Com foco ou conteúdo: rótulo elevado sobre uma abertura na borda superior. A transição dura aproximadamente 150 ms.
- A borda, o rótulo e a mensagem usam a cor do erro, com prioridade sobre a cor de foco. O estado desabilitado usa as variações de cor do tema.
- Uma linha fica reservada abaixo do campo mesmo sem erro. Mensagens longas quebram em mais linhas e aumentam a altura preferida; use um layout que respeite essa altura.
- A dica de máscara aparece quando o rótulo está completamente elevado. Sem rótulo, a dica permanece disponível normalmente.
- Ícone, botão de limpar, cursor e seleção continuam na área de digitação. Os campos de caminho mantêm seus breadcrumbs e o modo de edição.
- A troca do tema mantém o layout e a cor personalizada. Remover o campo da hierarquia interrompe a animação. Trocar ou desconectar o layout libera seus listeners e restaura a opacidade e a descrição acessível anteriores.
- A mensagem de erro é a descrição acessível do campo. Remover o erro restaura a descrição existente antes de instalar o layout.
- A estratégia não altera máscaras, valores, validação nem eventos. Use os contratos de eventos de cada campo: `JTextFieldListener` puro continua sendo uma base, e as subclasses emitem seus eventos existentes.

## Criar outra estratégia

`FieldLayoutManager`, no pacote `dtm.stools.component.inputfields.textfield.layout`, é um contrato de apresentação independente de `java.awt.LayoutManager`. Ele não substitui o layout dos filhos, como os breadcrumbs.

Os métodos recebem um `FieldLayoutTarget`, implementado por `JTextFieldListener` e `PasswordField`. Use `getFieldComponent()` para acessar o editor Swing, seus listeners, foco, fonte e dimensões. `getLabel()`, `isFieldContentEmpty()` e `getFieldContentBounds()` fornecem os dados do campo; no campo de senha, a verificação de vazio usa o comprimento do documento.

Implemente `getInsets`, `getFieldBounds` e `getPreferredSize`; opcionalmente personalize `getMinimumSize`, `paintBackground`, `paintBorder`, `paintOverlay` e `isPlaceholderVisible`. As margens excluem os espaços dos ícones e do botão de limpar, que são acrescentados pela classe base. A pintura do editor fica recortada à área útil; a decoração é pintada após os filhos.

Os hooks `install` / `uninstall`, `fieldChanged`, `fieldShown` / `fieldRemoved` e `themeChanged` permitem gerenciar estado e recursos. Uma implementação que adiciona listeners deve removê-los em `uninstall`, encerrar animações em `fieldRemoved` e desfazer instalações que falharem.

## Exemplo executável

`src/test/java/dtm/stools/examples/MaterialLayoutExample.java` demonstra campos simples, CPF com máscara, número, autocomplete, breadcrumbs, estado desabilitado, erro persistente, troca de cor, troca de tema e ativação/remoção do layout.
