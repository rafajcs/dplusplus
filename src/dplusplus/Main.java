package dplusplus;

import dplusplus.lexer.*;
import dplusplus.node.*;
import dplusplus.parser.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
    	// Arquivo padrão para testes rápidos
        String filePath = "teste/LinkedList.dpp"; 

        if (args.length >= 1) {
            filePath = args[0];
        } else {
            System.out.println("[INFO] Nenhum arquivo fornecido. Executando teste padrão: " + filePath);
            System.out.println("[DICA] Uso customizado: java dplusplus.Main <caminho_do_arquivo.dpp>\n");
        }

        try (FileReader fileReader = new FileReader(filePath);
             PushbackReader pushbackReader = new PushbackReader(fileReader, 1024)) {

            System.out.println("Processando o arquivo: " + filePath);

            // 1. Inicializa a Análise Léxica
            Lexer lexer = new Lexer(pushbackReader);

            // 2. Inicializa a Análise Sintática
            Parser parser = new Parser(lexer);

            // 3. Executa o parser (Retorna a raiz da AST devido às transformações {->})
            Start astRoot = parser.parse();
            System.out.println("[SINTÁTICA OK] Arquivo sintaticamente válido.");

            // 4. Inicializa o Analisador Semântico
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();

            // 5. Dispara o Visitor caminhando sobre os nós abstratos
            astRoot.apply(semanticAnalyzer);

            // 6. Avaliação dos resultados semânticos
            if (semanticAnalyzer.hasErrors()) {
                System.err.println("\n[SEMÂNTICA FALHOU] Foram detectados erros semânticos:");
                semanticAnalyzer.printErrors();
            } else {
                System.out.println("\n[SEMÂNTICA OK] Programa validado com sucesso! Iniciando geração de código...");
                // 7. Iniciar Geração de Código
                dplusplus.codegen.CodeGenerator codeGen = new dplusplus.codegen.CodeGenerator("generated_java");
                codeGen.generate(astRoot);
            }

        } catch (LexerException e) {
            System.err.println("[ERRO LÉXICO] Caractere inválido ou token mal formado: " + e.getMessage());
        } catch (ParserException e) {
            System.err.println("[ERRO SINTÁTICO] Quebra de regra de produção na linguagem: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("[ERRO DE LEITURA] Falha ao aceder ao ficheiro de teste: " + e.getMessage());
        }
    }
}