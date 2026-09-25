package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.function.SheetFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FunctionSignatures {
    private static final Map<String, String> PARAMS = new HashMap<>();
    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    static {
        p("SUM", "núm1|[núm2]|...", "Soma todos os números de um intervalo de células.");
        p("AVERAGE", "núm1|[núm2]|...", "Retorna a média aritmética dos argumentos.");
        p("COUNT", "valor1|[valor2]|...", "Calcula quantas células contêm números.");
        p("COUNTA", "valor1|[valor2]|...", "Calcula quantas células não estão vazias.");
        p("MAX", "núm1|[núm2]|...", "Retorna o valor máximo de um conjunto de valores.");
        p("MIN", "núm1|[núm2]|...", "Retorna o valor mínimo de um conjunto de valores.");
        p("IF", "teste_lógico|[valor_se_verdadeiro]|[valor_se_falso]", "Verifica se uma condição é satisfeita e retorna um valor se VERDADEIRO e outro se FALSO.");
        p("IFS", "teste_lógico1|valor_se_verdadeiro1|...", "Verifica várias condições e retorna o valor da primeira verdadeira.");
        p("IFERROR", "valor|valor_se_erro", "Retorna valor_se_erro se a expressão for um erro; caso contrário, o valor.");
        p("IFNA", "valor|valor_se_nd", "Retorna o valor especificado se a expressão resultar em #N/D.");
        p("AND", "lógico1|[lógico2]|...", "Retorna VERDADEIRO se todos os argumentos forem verdadeiros.");
        p("OR", "lógico1|[lógico2]|...", "Retorna VERDADEIRO se algum argumento for verdadeiro.");
        p("NOT", "lógico", "Inverte o valor lógico do argumento.");
        p("SUMIF", "intervalo|critérios|[intervalo_soma]", "Adiciona as células especificadas por um critério.");
        p("SUMIFS", "intervalo_soma|intervalo_critérios1|critérios1|...", "Adiciona as células que atendem a vários critérios.");
        p("COUNTIF", "intervalo|critérios", "Calcula o número de células que atendem a um critério.");
        p("COUNTIFS", "intervalo_critérios1|critérios1|...", "Conta células que atendem a vários critérios.");
        p("AVERAGEIF", "intervalo|critérios|[intervalo_média]", "Média das células que atendem a um critério.");
        p("AVERAGEIFS", "intervalo_média|intervalo_critérios1|critérios1|...", "Média das células que atendem a vários critérios.");
        p("VLOOKUP", "valor_procurado|matriz_tabela|núm_índice_coluna|[procurar_intervalo]", "Procura um valor na primeira coluna e retorna um valor na mesma linha.");
        p("HLOOKUP", "valor_procurado|matriz_tabela|núm_índice_lin|[procurar_intervalo]", "Procura um valor na primeira linha e retorna um valor na mesma coluna.");
        p("XLOOKUP", "valor_procurado|matriz_pesquisa|matriz_retorno|[se_não_encontrada]|[modo_correspondência]|[modo_pesquisa]", "Procura um intervalo ou matriz e retorna o item correspondente.");
        p("XMATCH", "valor_procurado|matriz_pesquisa|[modo_correspondência]|[modo_pesquisa]", "Retorna a posição relativa de um item em uma matriz.");
        p("MATCH", "valor_procurado|matriz_procurada|[tipo_correspondência]", "Retorna a posição relativa de um item em uma matriz.");
        p("INDEX", "matriz|núm_linha|[núm_coluna]|[núm_área]", "Retorna um valor ou referência em uma posição de um intervalo.");
        p("OFFSET", "ref|lins|cols|[altura]|[largura]", "Retorna uma referência deslocada de uma referência inicial.");
        p("INDIRECT", "texto_ref|[a1]", "Retorna a referência especificada por um texto.");
        p("CHOOSE", "núm_índice|valor1|[valor2]|...", "Escolhe um valor de uma lista com base em um índice.");
        p("CONCAT", "texto1|[texto2]|...", "Concatena uma lista ou intervalo de textos.");
        p("CONCATENATE", "texto1|[texto2]|...", "Agrupa vários itens de texto em um único item.");
        p("TEXTJOIN", "delimitador|ignorar_vazio|texto1|[texto2]|...", "Combina textos com um delimitador.");
        p("TEXT", "valor|formato_texto", "Converte um valor em texto em um formato de número específico.");
        p("LEFT", "texto|[núm_caract]", "Retorna os caracteres mais à esquerda de um texto.");
        p("RIGHT", "texto|[núm_caract]", "Retorna os caracteres mais à direita de um texto.");
        p("MID", "texto|núm_inicial|núm_caract", "Retorna caracteres do meio de um texto.");
        p("LEN", "texto", "Retorna o número de caracteres de um texto.");
        p("FIND", "texto_procurado|no_texto|[núm_inicial]", "Localiza um texto dentro de outro (diferencia maiúsculas).");
        p("SEARCH", "texto_procurado|no_texto|[núm_inicial]", "Localiza um texto dentro de outro (não diferencia maiúsculas).");
        p("SUBSTITUTE", "texto|texto_antigo|novo_texto|[núm_da_ocorrência]", "Substitui texto antigo por um novo texto.");
        p("REPLACE", "texto_antigo|núm_inicial|núm_caract|novo_texto", "Substitui parte de um texto.");
        p("TRIM", "texto", "Remove os espaços extras do texto.");
        p("UPPER", "texto", "Converte o texto em maiúsculas.");
        p("LOWER", "texto", "Converte o texto em minúsculas.");
        p("PROPER", "texto", "Primeira letra de cada palavra em maiúscula.");
        p("VALUE", "texto", "Converte um texto que representa um número em número.");
        p("DATE", "ano|mês|dia", "Retorna o número de série de uma data.");
        p("TIME", "hora|minuto|segundo", "Retorna o número de série de uma hora.");
        p("TODAY", "", "Retorna a data atual.");
        p("NOW", "", "Retorna a data e a hora atuais.");
        p("YEAR", "núm_série", "Retorna o ano de uma data.");
        p("MONTH", "núm_série", "Retorna o mês de uma data.");
        p("DAY", "núm_série", "Retorna o dia de uma data.");
        p("WEEKDAY", "núm_série|[tipo_retorno]", "Retorna o dia da semana.");
        p("EDATE", "data_inicial|meses", "Retorna a data que está a um número de meses de outra.");
        p("EOMONTH", "data_inicial|meses", "Retorna o último dia do mês.");
        p("NETWORKDAYS", "data_inicial|data_final|[feriados]", "Retorna o número de dias úteis entre duas datas.");
        p("WORKDAY", "data_inicial|dias|[feriados]", "Retorna a data depois de um número de dias úteis.");
        p("DATEDIF", "data_inicial|data_final|unidade", "Calcula a diferença entre duas datas.");
        p("ROUND", "núm|núm_dígitos", "Arredonda um número até uma quantidade de dígitos.");
        p("ROUNDUP", "núm|núm_dígitos", "Arredonda um número para cima.");
        p("ROUNDDOWN", "núm|núm_dígitos", "Arredonda um número para baixo.");
        p("INT", "núm", "Arredonda um número para baixo até o inteiro mais próximo.");
        p("MOD", "núm|divisor", "Retorna o resto da divisão.");
        p("ABS", "núm", "Retorna o valor absoluto de um número.");
        p("POWER", "núm|potência", "Retorna o resultado de um número elevado a uma potência.");
        p("SQRT", "núm", "Retorna a raiz quadrada.");
        p("PRODUCT", "núm1|[núm2]|...", "Multiplica os números.");
        p("SUMPRODUCT", "matriz1|[matriz2]|...", "Retorna a soma dos produtos dos componentes correspondentes.");
        p("SUBTOTAL", "núm_função|ref1|...", "Retorna um subtotal em uma lista.");
        p("RAND", "", "Retorna um número aleatório entre 0 e 1.");
        p("RANDBETWEEN", "inferior|superior", "Retorna um número aleatório entre os números especificados.");
        p("MEDIAN", "núm1|[núm2]|...", "Retorna a mediana.");
        p("STDEV.S", "núm1|[núm2]|...", "Estima o desvio padrão de uma amostra.");
        p("LARGE", "matriz|k", "Retorna o k-ésimo maior valor.");
        p("SMALL", "matriz|k", "Retorna o k-ésimo menor valor.");
        p("RANK.EQ", "núm|ref|[ordem]", "Retorna a posição de um número em uma lista.");
        p("FILTER", "matriz|incluir|[se_vazia]", "Filtra um intervalo com base em critérios.");
        p("SORT", "matriz|[índice_classificação]|[ordem_classificação]|[por_col]", "Classifica o conteúdo de um intervalo ou matriz.");
        p("SORTBY", "matriz|por_matriz1|[ordem1]|...", "Classifica um intervalo com base nos valores de outro.");
        p("UNIQUE", "matriz|[por_col]|[exatamente_uma_vez]", "Retorna os valores exclusivos de um intervalo.");
        p("SEQUENCE", "linhas|[colunas]|[início]|[etapa]", "Gera uma lista de números sequenciais.");
        p("LET", "nome1|valor_nome1|cálculo_ou_nome2|...", "Atribui nomes a resultados de cálculo.");
        p("LAMBDA", "[parâmetro1]|...|cálculo", "Cria uma função personalizada reutilizável.");
        p("PMT", "taxa|nper|vp|[vf]|[tipo]", "Calcula o pagamento de um empréstimo.");
        p("PV", "taxa|nper|pgto|[vf]|[tipo]", "Retorna o valor presente de um investimento.");
        p("FV", "taxa|nper|pgto|[vp]|[tipo]", "Retorna o valor futuro de um investimento.");
        p("NPV", "taxa|valor1|[valor2]|...", "Retorna o valor presente líquido.");
        p("IRR", "valores|[estimativa]", "Retorna a taxa interna de retorno.");
        p("RATE", "nper|pgto|vp|[vf]|[tipo]|[estimativa]", "Retorna a taxa de juros por período.");
        p("QUERY", "dados|consulta|[cabeçalhos]", "Executa uma consulta na linguagem de consultas da API de visualização do Google.");
        p("SPLIT", "texto|delimitador|[dividir_por_caractere]|[remover_texto_vazio]", "Divide um texto ao redor de um delimitador.");
        p("ARRAYFORMULA", "fórmula_matricial", "Permite exibir valores retornados de uma fórmula matricial.");
        p("HYPERLINK", "local_vínculo|[nome_amigável]", "Cria um atalho para um local.");
        p("ISBLANK", "valor", "Verifica se uma célula está vazia.");
        p("ISNUMBER", "valor", "Verifica se um valor é um número.");
        p("ISERROR", "valor", "Verifica se um valor é um erro.");
        p("TRANSPOSE", "matriz", "Converte linhas em colunas e vice-versa.");
    }

    private FunctionSignatures() {}

    private static void p(String name, String params, String description) { PARAMS.put(name, params); DESCRIPTIONS.put(name, description); }

    public static List<String> parameters(SheetFunction f) {
        String p = PARAMS.get(f.name());
        if (p != null) return p.isEmpty() ? List.of() : List.of(p.split("\\|"));
        if (!f.parameters().isEmpty()) return f.parameters();
        List<String> list = new java.util.ArrayList<>();
        int shown = Math.min(Math.max(f.minArgs(), 1), 4);
        if (f.maxArgs() == 0) return List.of();
        for (int k = 1; k <= shown; k++) list.add(k <= f.minArgs() ? "arg" + k : "[arg" + k + "]");
        if (f.maxArgs() > shown) list.add("...");
        return list;
    }

    public static String description(SheetFunction f) {
        String d = DESCRIPTIONS.get(f.name());
        return d != null ? d : f.description().isEmpty() ? f.category().label() : f.description();
    }
}
