# Documentação da Geração de Código (D++ para Java)

A arquitetura de geração de código foi construída com foco em isolamento de responsabilidades (Princípio de Responsabilidade Única - SRP). Caso a análise semântica sofra alterações (como a adição de novos tipos, validações rígidas ou checagens de tabela de símbolos), o gerador de código foi projetado para atuar como um **Back-end "Puro"**. Ou seja, ele confia que a AST que chegou até ele já é válida e foca estritamente na tradução sintática.

Abaixo estão detalhadas as três classes principais adicionadas no pacote `dplusplus.codegen`:

## 1. `CodeGenerator` (O Orquestrador)

* **O que é:** A classe de entrada (Entry Point) para a fase de geração de código.
* **Responsabilidade:** Receber o sinal verde da `Main.java` e inicializar o processo de tradução, definindo o diretório de saída.
* **Por que isolar?** Se futuramente for necessário acoplar a invocação do compilador nativo do Java (`javac`), passar argumentos para o SO, gerenciar a criação da pasta de saída ou até instanciar múltiplos arquivos baseados na Tabela de Símbolos, tudo isso ocorrerá aqui, sem poluir a lógica de travessia da árvore.
* **Como manter:** Modifique esta classe caso precise alterar o fluxo macro de execução, como "onde" os arquivos serão salvos ou etapas de pré/pós-processamento da tradução (ex: apagar uma pasta de build antiga).

## 2. `CodeGeneratorVisitor` (O Tradutor Estrutural)

* **O que é:** O *Visitor Principal* que herda de `DepthFirstAdapter` do SableCC.
* **Responsabilidade:** Mapear a **Estrutura de Controle e Declarações** da AST do D++ para a sintaxe do Java (Arquivos, Classes, Funções, Condicionais e Laços).
* **Como funciona:** Ele percorre a árvore top-down escutando apenas os nós estruturais:
  * Ao encontrar um nó de *Classe* (`family`), cria um contexto (`StringBuilder`) e se prepara para salvar um novo arquivo `.java`.
  * Ao encontrar uma *Função* ou *Main* (`procedure`), estrutura a assinatura do método (`public static ...`), abre chaves `{` e gerencia a indentação.
  * Ao encontrar um bloco de controle (`case that` / `as long as`), traduz para `if`/`while` com seus respectivos fechamentos de bloco.
* **Como manter:** Se adicionar uma nova estrutura de bloco ao D++ (como `for` ou `switch`), é aqui que você criará um novo método (ex: `caseAForComando`) para construir a casca do comando em Java.

## 3. `ExpressionTranslator` (O Avaliador de Expressões Inline)

* **O que é:** Um *Visitor Auxiliar*, também herdeiro de `DepthFirstAdapter`.
* **Responsabilidade:** Avaliar e "achatar" **Expressões Matemáticas, Lógicas e Chamadas de Métodos**, retornando a instrução completa em uma única `String`.
* **Por que isolar?** Expressões na AST são extremamente recursivas (ex: `a + b * (c -> get[])`). Formatar isso dentro do Visitor Principal resultaria em quebras de linha indesejadas e um controle de estado complexo. Este tradutor atua de forma autônoma: recebe a raiz de uma expressão, navega pelos nós folha e retorna o texto correspondente de forma linear.
* **Ponto de destaque:** Realiza o "achatamento" de prefixos, convertendo o encadeamento de objetos D++ (`objeto->metodo[]`) para o padrão Java (`objeto.metodo()`).
* **Como manter:** Modificações envolvendo novos operadores matemáticos/lógicos, tratamento especial de literais ou mudanças na sintaxe de acesso a atributos devem ser feitas **exclusivamente** nesta classe. O Visitor Principal permanece agnóstico a essas mudanças.

---

## 🔄 Impacto de Mudanças Semânticas

Graças a esse isolamento, a maioria das atualizações na fase semântica não quebrará o gerador de código. Contudo, existem cenários onde o código gerador precisará ser ajustado:

1. **Novos Tipos:** Se a semântica aprovar a criação de um novo tipo no D++ (como `string`), será necessário atualizar o método auxiliar de mapeamento (ex: `mapType()`) no `CodeGeneratorVisitor` para a tradução correta no Java.
2. **Coerção de Tipos e Tabela de Símbolos:** Atualmente, a tradução é puramente baseada na AST. Se a semântica decidir injetar conversões implícitas (castings), talvez seja necessário passar a `SymbolTable` para dentro do `CodeGeneratorVisitor` ou `ExpressionTranslator` para tomar decisões baseadas nos tipos inferidos.

Com a arquitetura dividida em "Orquestração", "Estrutura" e "Expressões", a evolução da linguagem D++ possui uma base sólida e facilmente modularizável.
