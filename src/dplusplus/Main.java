package dplusplus;

import dplusplus.lexer.*;
import dplusplus.node.*;
import dplusplus.parser.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        String filePath = "teste/fatorial.dpp";
        if (args.length >= 1) {
            filePath = args[0];
        }

        try (FileReader fileReader = new FileReader(filePath);
             PushbackReader pushbackReader = new PushbackReader(fileReader, 1024)) {

            System.out.println("A abrir e a processar o ficheiro: " + filePath);

            // 1. Inicializa a Análise Léxica
            Lexer lexer = new Lexer(pushbackReader);

            // 2. Inicializa a Análise Sintática
            Parser parser = new Parser(lexer);

            // 3. Executa o parser (Retorna a raiz da AST devido às transformações {->})
            Start astRoot = parser.parse();
            System.out.println("[SINTÁTICA OK] Ficheiro sintaticamente válido.");

            // 4. Inicializa o Analisador Semântico
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();

            // 5. Dispara o Visitor caminhando sobre os nós abstratos
            astRoot.apply(semanticAnalyzer);

            // 6. Avaliação dos resultados semânticos
            if (semanticAnalyzer.hasErrors()) {
                System.err.println("\n[SEMÂNTICA FALHOU] Foram detetados erros semânticos:");
                semanticAnalyzer.printErrors();
            } else {
                System.out.println("\n[SEMÂNTICA OK] Programa validado com sucesso! Pronto para geração de código.");
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