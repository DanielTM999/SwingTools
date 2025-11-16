# 📘 Documentação da Classe `MaskedTextField`

## 🌟 Visão Geral

A classe **MaskedTextField** é um componente base projetado para
padronizar e simplificar a criação de campos de entrada de texto com
comportamentos especiais.\
Ela oferece uma infraestrutura que facilita desde simples validações até
inputs formatados com máscaras complexas.

Essa classe não é usada diretamente pelo usuário final --- ela serve
como **fundação** para outros componentes visuais mais sofisticados.

------------------------------------------------------------------------

## 🎯 Propósito da Classe

A `MaskedTextField` foi criada com os seguintes objetivos:

-   **Centralizar comportamentos comuns de campos de texto**\
    Para evitar duplicação de lógica em diversas subclasses.

-   **Fornecer suporte interno a máscaras**\
    Permitindo criar campos como CPF, CNPJ, telefone, CEP, datas e
    outros formatos estruturados.

-   **Simplificar validações e manipulação de eventos**\
    Deixando subclasses apenas com a lógica realmente específica delas.

-   **Servir como uma classe extensível, segura e estável**\
    Criada para ser herdada com fluidez.

------------------------------------------------------------------------

## 🔧 Como Funciona

A classe funciona como um campo de entrada inteligente.\
Ela oferece mecanismos para:

### ✔️ Aplicação de Máscaras

Permite representar e validar o texto digitado conforme um padrão
predefinido.\
Exemplos de máscaras:\
- `###.###.###-##` (CPF)\
- `(##) #####-####` (telefone)\
- `##/##/####` (data)

### ✔️ Manipulação unificada de eventos

Métodos que facilitam lidar com:\
- Alterações de texto\
- Foco e desfoco\
- Teclas pressionadas\
- Verificações de consistência do valor

### ✔️ Validação automática

As subclasses podem ativar ou sobrescrever métodos para verificar se o
conteúdo está completo, coerente ou formatado corretamente.

------------------------------------------------------------------------

## 🏗️ Como Estender esta Classe

Para criar um novo componente baseado nela, basta estender a classe e
sobrescrever o que for necessário:

### 🎨 1. Definir a máscara (se houver)

A subclasse informa como o texto deve se comportar e se autoformatar.

### 🔍 2. Implementar validação específica

Cada tipo de input pode exigir suas próprias regras.\
Exemplo: validar se o CPF é válido.

### 🧠 3. Sobrescrever métodos de eventos

Permite modificar:\
- Como o texto é interpretado\
- Como a máscara é aplicada\
- Como o cursor se movimenta\
- Como erros são apresentados

### 💡 4. Adicionar comportamento visual

Como cores, placeholders, dicas, alertas ou ícones.

------------------------------------------------------------------------

## 📦 Exemplos de Componentes que Podem Herdar de `MaskedTextField`

### `CpfField`

-   Aplica máscara automaticamente\
-   Valida o dígito verificador\
-   Impede entrada de caracteres inválidos

### `TelefoneField`

-   Formata automaticamente durante a digitação\
-   Adapta o formato para telefones com 8 ou 9 dígitos

### `DateField`

-   Auxilia o usuário na digitação\
-   Garante formatação de dia/mês/ano

### `CepField`

-   Máscara `#####-###`\
-   Pode até realizar consulta automática em uma API externa

------------------------------------------------------------------------

## 🧰 Benefícios de Usar Esta Classe Como Base

### ⭐ **Padronização**

Todos os campos formatados seguem um mesmo fluxo e comportamento
interno.

### ⭐ **Menos Código Repetido**

Boa parte da lógica complicada já está implementada na classe base.

### ⭐ **Manutenção Simplificada**

Alterar a lógica base beneficia todos os componentes que herdaram dela.

### ⭐ **Maior Reutilização**

Ideal para bibliotecas de UI internas ou frameworks proprietários.

------------------------------------------------------------------------

## 🚀 Quando Usar

Use `MaskedTextField` como base quando você precisar criar:

-   Inputs com formatação automática\
-   Inputs com validações específicas\
-   Campos complexos que exigem manipulação profunda do texto\
-   Componentes visuais reutilizáveis para sua aplicação
------------------------------------------------------------------------

### Caracteres Especiais da Máscara

Quando uma máscara é definida, os seguintes caracteres têm significado especial:

| Caractere | Descrição |
| :--- | :--- |
| **`#`** | Representa um dígito (número) |
| **`U`** | Representa uma letra (converte para maiúscula) |
| **`L`** | Representa uma letra (converte para minúscula) |
| **`$`** | Representa uma letra ou dígito (converte para maiúscula) |
| **`@`** | Representa uma letra ou dígito (converte para minúscula) |
| **`&`** | Representa uma letra ou dígito (sem conversão) |
| **`?`** | Representa uma letra (sem conversão) |
| **`*`** | Representa qualquer caractere |

Qualquer outro caractere na string da máscara é tratado como um **literal**, o que significa que ele será exibido no campo e não poderá ser removido pelo usuário (ex: `.`, `-`, `/`, `(`).

------------------------------------------------------------------------

---

### 🚀 Modo de Uso e Construtores

Você pode usar este componente com ou sem uma máscara.

#### 1. Com Máscara

Usado para entradas formatadas como CPF, CNPJ, datas, telefones, etc.

```java
// Exemplo de máscara de CPF
MaskedTextField cpfField = new MaskedTextField("###.###.###-##");

// Exemplo de máscara de Data com caractere de placeholder customizado
MaskedTextField dateField = new MaskedTextField("##/##/####", ' ');
```