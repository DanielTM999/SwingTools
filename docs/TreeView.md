# TreeView

`TreeView<T>` e uma arvore Swing orientada a dados de dominio. Ela usa `TreeNode<T>` como no e adiciona recursos como selecao, busca, filtro, check state, lazy load, edicao, popup e drag and drop.

| Item | Valor |
|---|---|
| Pacote | `dtm.stools.component.tree` |
| Heranca | `TreeView<T> extends TreeViewListener` |
| Base Swing | `JTree` |
| No de dominio | `TreeNode<T>` |

## Modelo mental

O dado real fica em `TreeNode<T>#data`. A label exibida pode vir de `label`, de `labelProvider` ou de `String.valueOf(data)`.

```java
TreeNode<String> root = new TreeNode<>("workspace", "workspace");
TreeView<String> tree = new TreeView<>(root);
```

## TreeNode

Campos e comportamentos importantes:

| Propriedade | Uso |
|---|---|
| `id` | Identidade logica; gerada automaticamente por UUID |
| `data` | Objeto de dominio |
| `label` | Texto fixo exibido |
| `icon` / `iconProvider` | Icone fixo ou calculado |
| `tooltip`, `foreground`, `background`, `font` | Aparencia |
| `selectable`, `enabled`, `editable` | Interacao |
| `checkable`, `checkState` | Checkbox |
| `lazy`, `childrenProvider`, `loaded`, `loading` | Carregamento sob demanda |
| `draggable`, `dropTarget` | Drag and drop |
| `popupMenuProvider` | Menu contextual por no |

## Estrutura

| Metodo | Contrato |
|---|---|
| `setRoot(TreeNode<T>)` | Troca a raiz |
| `addNode(parent, value)` | Cria e adiciona um filho |
| `addNode(parent, child)` | Adiciona um `TreeNode` existente |
| `removeNode(TreeNode<T>)` | Remove no |
| `moveNode(node, newParent, index)` | Move no |
| `refreshNode(TreeNode<T>)` | Atualiza model/UI |

## Selecao e navegacao

Use `TreeViewMode.SINGLE`, `MULTIPLE` ou `DISCONTIGUOUS` conforme a necessidade.

APIs principais: `selectNode`, `selectNodes`, `getSelectedNode`, `getSelectedNodes`, `revealNode`, `expandParents`, `expandAll`, `collapseAll`, `expandToDepth`, `expandTo`, `snapshotExpansion` e `restoreExpansion`.

## Busca, filtro e atualizacao

```java
tree.updateNodes(
        node -> node.computeLabel().endsWith(".java"),
        node -> node.setForeground(new Color(0x2563EB))
);

tree.queueUpdateNodes(
        node -> node.computeLabel().endsWith(".md"),
        node -> node.setForeground(new Color(0x059669))
);
tree.queueUpdateNodes(
        node -> node.computeLabel().equals("README.md"),
        node -> node.setLabel("README.md (docs)")
);
int changed = tree.applyQueuedNodeUpdates();

TreeNode<String> readme = tree.findByData(data -> "docs/README.md".equals(data));
```

`updateNodes` aplica na hora. `queueUpdateNodes` acumula alteracoes e `applyQueuedNodeUpdates` aplica o lote em um unico refresh.

APIs relacionadas: `setFilter`, `clearFilter`, `search`, `findMatches`, `findById`, `findByData`, `updateNodes`, `updateFirstNode`, `queueUpdateNodes`, `applyQueuedNodeUpdates`, `clearQueuedNodeUpdates`.

## Check state

```java
node.setCheckable(true);
tree.setNodeCheckState(node, CheckState.CHECKED);
List<TreeNode<String>> checked = tree.getCheckedNodes();
```

`CheckState` pode ser `UNCHECKED`, `CHECKED` ou `INDETERMINATE`.

## Lazy load

```java
TreeNode<Path> src = new TreeNode<>(Path.of("src"), "src");
src.setLazy(true);
src.setChildrenProvider(parent -> Files.list(parent.getData())
        .map(path -> new TreeNode<>(path, path.getFileName().toString()))
        .toList());
```

Use lazy load para diretorios, projetos grandes ou estruturas remotas.

## Exemplo completo

```java
TreeNode<String> root = new TreeNode<>("workspace", "workspace");
TreeView<String> tree = new TreeView<>(root);

TreeNode<String> src = tree.addNode(root, new TreeNode<>("src", "src"));
tree.addNode(src, new TreeNode<>("src/Main.java", "Main.java"));
tree.addNode(src, new TreeNode<>("src/AppService.java", "AppService.java"));

tree.expandAll();
tree.addEventListner(EventType.SELECT, event -> {
    TreeNode<String> selected = tree.getSelectedNode();
    if (selected != null) {
        System.out.println(selected.getData());
    }
});
```

## Cuidados

- Altere `TreeNode` pela API do `TreeView` quando precisar que a UI atualize.
- Em lazy load, trate excecoes do provider e sinalize erro na UI se necessario.
- Para filtros, preserve um caminho de restauracao ou use `clearFilter`.
- Como a base e `JTree`, renderers e modelos Swing ainda importam.
