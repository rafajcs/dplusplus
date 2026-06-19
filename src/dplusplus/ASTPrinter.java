package dplusplus;

import dplusplus.analysis.DepthFirstAdapter;
import dplusplus.node.Node;
import dplusplus.node.Token;

/**
 * Impressor genérico da árvore sintática.
 *
 * Independe da gramática: usa os ganchos defaultIn/defaultOut (nós de produção)
 * e defaultCase (tokens/folhas) do DepthFirstAdapter. Continua funcionando após
 * o parser ser regenerado pelo SableCC, sem precisar de ajustes.
 */
public class ASTPrinter extends DepthFirstAdapter {

	private int nivel = 0;

	private String indentacao() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nivel; i++) {
			sb.append("  ");
		}
		return sb.toString();
	}

	/** Remove o prefixo 'A' que o SableCC adiciona às produções alternativas. */
	private String nomeProducao(Node node) {
		String nome = node.getClass().getSimpleName();
		if (nome.length() > 1 && nome.charAt(0) == 'A' && Character.isUpperCase(nome.charAt(1))) {
			return nome.substring(1);
		}
		return nome;
	}

	@Override
	public void defaultIn(Node node) {
		System.out.println(indentacao() + nomeProducao(node));
		nivel++;
	}

	@Override
	public void defaultOut(Node node) {
		nivel--;
	}

	@Override
	public void defaultCase(Node node) {
		if (node instanceof Token) {
			Token token = (Token) node;
			String lexema = token.getText()
					.replace("\n", "\\n")
					.replace("\r", "\\r")
					.replace("\t", "\\t");
			System.out.println(indentacao() + token.getClass().getSimpleName() + " (\"" + lexema + "\")");
		}
	}
}
