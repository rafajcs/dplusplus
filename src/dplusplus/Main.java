package dplusplus;

import dplusplus.lexer.*;
import dplusplus.node.*;
import java.io.*;

public class Main {

	public static void main(String[] args) {

		String arquivo = args.length > 0 ? args[0] : "teste/LinkedList.dpp";

		try (PushbackReader reader = new PushbackReader(new FileReader(arquivo), 1024)) {

			Lexer lexer = new Lexer(reader);
			Token token;

			while (!((token = lexer.next()) instanceof EOF)) {
				if (token instanceof TVazio || token instanceof TComentLinha || token instanceof TComentBloco) {
					continue;
				}
				String tipo = token.getClass().getSimpleName();
				String lexema = token.getText().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
				System.out.printf("<%s, \"%s\"> linha %d, coluna %d%n",
						tipo, lexema, token.getLine(), token.getPos());
			}

		} catch (LexerException e) {
			System.err.println("ERRO LEXICO: " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.err.println("Arquivo nao encontrado: " + arquivo);
		} catch (IOException e) {
			System.err.println("Erro de IO: " + e.getMessage());
		}
	}
}
