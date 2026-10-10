# Acessibilidade dos campos

Os campos personalizados combinam controles Swing nativos com semântica explícita para elementos desenhados. Esta revisão cobre os campos de entrada e os novos componentes de senha, período e paleta; não representa homologação completa dos editores, gráficos, docking ou janelas.

| Controle | Semântica exposta |
|---|---|
| Checkbox / switch / radio | Papéis específicos, estado marcado e ação acessível; checkbox também informa indeterminado |
| Radio group / segmented | Filhos nomeados e seleção única acessível; segmentos oferecem opções virtuais |
| Slider / rating | Papel slider, valor corrente, limites e ações de aumentar/diminuir |
| Stepper | Papel spin box, valor, limites e botões acessíveis por teclado |
| Pin | Texto acessível; quando mascarado, somente caracteres de máscara e papel de senha |
| Texto / busca / textarea | Semântica nativa de texto; ação acessível de limpar nos campos com esse botão |
| Dual list | Listas nativas nomeadas e botões de transferência/reordenação acessíveis |
| Tags / cor / data | Controles nativos nomeados; botões de adicionar, remover e selecionar participam do foco |
| Senha / período / paleta | Controles nativos identificados, calendário com dias nomeados e lista de comandos acessível |

Alterar um valor com `fireEvent=false` suprime eventos da biblioteca, mas mantém as notificações acessíveis. Controles desabilitados não executam ações; rating e stepper respeitam somente leitura. Nos grupos de radio, setas navegam entre opções habilitadas. Botões desenhados aceitam Espaço/Enter e exibem foco. Tags deixam Tab/Shift+Tab para a navegação do formulário; Enter confirma uma tag.

`SwitchField` passa a desenhar foco por padrão. `setFocusPainted(false)` mantém a opção de desativá-lo; quando habilitado, a geometria reserva espaço interno para o contorno.

`FormField` associa rótulo ao controle, identifica o grupo e comunica ajuda/erro como descrição acessível. Nomes e descrições explicitamente definidos pelo aplicativo são preservados. Em componentes compostos, personalize também o controle de entrada nativo quando necessário:

```java
PasswordField senha = new PasswordField();
senha.getPasswordField().getAccessibleContext().setAccessibleName("Senha de acesso");
```

## Verificação

`NewControlsTest` verifica papéis, estados, ações, valores, seleção e notificações. `NewControlsVisualTest` gera imagens em `target/new-controls` nos temas claro/escuro e escalas 100%, 150% e 200%. `NewControlsWindowTest` verifica o ciclo de vida de popups e providers quando há ambiente gráfico.

Para homologação com leitor de tela no Windows, abra os exemplos e percorra os campos por Tab/Shift+Tab, opere botões e seletores, altere estados e confirme anúncios de rótulos, valor, marcado/desmarcado e erro. Essa verificação humana depende do leitor de tela e da ponte Java Access Bridge disponíveis no ambiente; os testes automatizados não substituem essa homologação.
