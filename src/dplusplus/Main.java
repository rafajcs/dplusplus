package dplusplus;

import dplusplus.lexer.*;
import dplusplus.node.*;
import dplusplus.parser.*;
import java.io.*;

public class Main {

	public static void main(String[] args) {

		String arquivo = args.length > 0 ? args[0] : "teste/procedure.dpp";
		
		//arquivo = arquivo + "sucesso/A1_aritmetica.dpp";

		try (PushbackReader reader = new PushbackReader(new FileReader(arquivo), 1024)) {

			Lexer lexer = new Lexer(reader);
			System.out.println("LEXER OK.");
			Parser parser = new Parser(lexer);
			
			

			Start arvore = parser.parse();

			System.out.println("Analise sintatica concluida com sucesso.");
			System.out.println("Arvore sintatica:");
			arvore.apply(new ASTPrinter());

		} catch (ParserException e) {
			System.err.println("ERRO SINTATICO: " + e.getMessage());
		} catch (LexerException e) {
			System.err.println("ERRO LEXICO: " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.err.println("Arquivo nao encontrado: " + arquivo);
		} catch (IOException e) {
			System.err.println("Erro de IO: " + e.getMessage());
		}
	}
}
