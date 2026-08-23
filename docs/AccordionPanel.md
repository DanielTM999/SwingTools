# AccordionPanel e SectionPanel

`SectionPanel` e uma secao colapsavel com cabecalho clicavel, seta animada e transicao de altura. `AccordionPanel` agrupa varias secoes e, opcionalmente, mantem apenas uma expandida por vez.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.panels.accordion` |
| Heranca | `SectionPanel extends PanelEventListener`, `AccordionPanel extends PanelEventListener` |
| Uso principal | Formularios longos e paineis de configuracao |

```java
AccordionPanel accordion = new AccordionPanel(true);
accordion.addSection("Dados pessoais", dadosPanel)
         .addSection("Endereco", enderecoPanel)
         .addSection("Preferencias", preferenciasPanel);

accordion.addEventListener(AccordionPanel.SECTION_CHANGED, e -> {
    SectionPanel secao = e.tryGetValue();
});
```

## AccordionPanel

| Metodo | Contrato |
|---|---|
| `addSection(String title, JComponent content)` | Cria e adiciona uma secao |
| `addSection(SectionPanel)` | Adiciona uma secao ja construida |
| `clearSections()` | Remove todas |
| `setExclusive(boolean)` / `isExclusive()` | Apenas uma expandida por vez |
| `expandSection(int)` | Expande pelo indice |
| `collapseAll()` | Colapsa todas, sem disparar eventos por secao |
| `getSections()` | Copia imutavel das secoes |

Em modo exclusivo, a primeira secao adicionada nasce expandida e as demais colapsadas. Expandir uma secao colapsa as outras sem emitir eventos redundantes.

Eventos do grupo: `AccordionPanel.SECTION_CHANGED` e `EventType.CHANGE`.

## SectionPanel

| Metodo | Contrato |
|---|---|
| `setContent(JComponent)` | Conteudo exibido quando expandida |
| `setTitle(String)` / `getTitle()` | Titulo do cabecalho |
| `setSubtitle(String)` | Texto auxiliar a direita do cabecalho |
| `isExpanded()` / `setExpanded(boolean)` | Estado, disparando eventos |
| `setExpanded(boolean, boolean fireEvent)` | Estado controlando o disparo |
| `toggle()` | Alterna |
| `setAnimated(boolean)` / `setAnimationDuration(int)` | Transicao de altura |
| `setHeaderHeight(int)` / `setArc(int)` | Geometria |
| `setColors(Color background, Color header, Color border)` | Cores principais |

Eventos da secao: `SectionPanel.EXPANDED`, `SectionPanel.COLLAPSED`, `EventType.EXPAND`, `EventType.COLLAPSE` e `EventType.CHANGE`.

O cabecalho e focavel: `Espaco` e `Enter` alternam a secao. A seta gira conforme o progresso da animacao, entao o estado intermediario tambem fica coerente.

`getPreferredSize()` interpola a altura do conteudo durante a animacao e nunca fica abaixo da altura do cabecalho.
