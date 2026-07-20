package dplusplus;

import dplusplus.analysis.DepthFirstAdapter;
import dplusplus.node.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticAnalyzer extends DepthFirstAdapter {
    private SymbolTable symbolTable;
    private List<String> errorList; // Acumulador de mensagens de erro

    // Mapa de Tipos: associa nós da AST aos seus respectivos tipos inferidos
    private final Map<Node, SymbolInfo.TypeKind> nodeTypes = new HashMap<>();

    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.errorList = new ArrayList<>();
    }

    // Regista um novo erro sem interromper o caminhamento da árvore
    public void reportError(String message, int line, int pos) {
        errorList.add("Erro Semântico [" + line + "," + pos + "]: " + message);
    }

    public void reportError(String message, Node node) {
        Token t = getFirstToken(node);
        int line = t != null ? t.getLine() : 0;
        int pos = t != null ? t.getPos() : 0;
        reportError(message, line, pos);
    }

    public boolean hasErrors() {
        return !errorList.isEmpty();
    }

    public void printErrors() {
        for (String error : errorList) {
            System.err.println("  -> " + error);
        }
    }

    public Map<Node, SymbolInfo.TypeKind> getNodeTypes() {
        return nodeTypes;
    }

    // Utilitários

    private Token getFirstToken(Node node) {
        if (node == null) return null;
        if (node instanceof Token) return (Token) node;

        final Token[] found = new Token[1];
        node.apply(new DepthFirstAdapter() {
            @Override
            public void defaultIn(Node n) {
                if (found[0] == null && n instanceof Token) {
                    found[0] = (Token) n;
                }
            }
            @Override
            public void defaultCase(Node n) {
                if (found[0] == null && n instanceof Token) {
                    found[0] = (Token) n;
                }
            }
        });
        return found[0];
    }

    // INFERÊNCIA E MAPA DE TIPOS

    @Override
    public void outAInteiroExpressao(AInteiroExpressao node) {
        nodeTypes.put(node, SymbolInfo.TypeKind.INTEIRO);
    }

    @Override
    public void outARealExpressao(ARealExpressao node) {
        nodeTypes.put(node, SymbolInfo.TypeKind.INTEIRO);
    }

    @Override
    public void outAVerdadeiroExpressao(AVerdadeiroExpressao node) {
        nodeTypes.put(node, SymbolInfo.TypeKind.BOOLEANO);
    }

    @Override
    public void outAFalsoExpressao(AFalsoExpressao node) {
        nodeTypes.put(node, SymbolInfo.TypeKind.BOOLEANO);
    }

    @Override
    public void outAIdExpressao(AIdExpressao node) {
        String id = node.getId().getText().trim();
        SymbolInfo info = symbolTable.lookup(id);

        if (info == null) {
            reportError("Identificador '" + id + "' não foi declarado.", node.getId().getLine(), node.getId().getPos());
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        } else {
            nodeTypes.put(node, info.getTypeKind());
        }
    }

    // Aritméticas

    @Override
    public void outASomaExpressao(ASomaExpressao node) {
        validaOperacaoBinariaAritmetica(node, node.getEsq(), node.getDir(), "soma");
    }

    @Override
    public void outASubtracaoExpressao(ASubtracaoExpressao node) {
        validaOperacaoBinariaAritmetica(node, node.getEsq(), node.getDir(), "subtração");
    }

    @Override
    public void outAMultExpressao(AMultExpressao node) {
        validaOperacaoBinariaAritmetica(node, node.getEsq(), node.getDir(), "multiplicação");
    }

    @Override
    public void outADivExpressao(ADivExpressao node) {
        validaOperacaoBinariaAritmetica(node, node.getEsq(), node.getDir(), "divisão");
    }

    @Override
    public void outAMenosUnarioExpressao(AMenosUnarioExpressao node) {
        SymbolInfo.TypeKind t = nodeTypes.get(node.getExpressao());
        if (t == SymbolInfo.TypeKind.ERRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        } else if (t == SymbolInfo.TypeKind.INTEIRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.INTEIRO);
        } else {
            reportError("Operador unário '-' exige operando do tipo INTEIRO, mas encontrou " + t + ".", node);
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        }
    }

    // Lógica e Negação

    @Override
    public void outANegacaoExpressao(ANegacaoExpressao node) {
        SymbolInfo.TypeKind t = nodeTypes.get(node.getExpressao());
        if (t == SymbolInfo.TypeKind.ERRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        } else if (t == SymbolInfo.TypeKind.BOOLEANO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.BOOLEANO);
        } else {
            reportError("Operador de negação '!' exige operando do tipo BOOLEANO, mas encontrou " + t + ".", node);
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        }
    }

    @Override
    public void outAEExpressao(AEExpressao node) {
        validaOperacaoBinariaLogica(node, node.getEsq(), node.getDir(), "and");
    }

    @Override
    public void outAOuExpressao(AOuExpressao node) {
        validaOperacaoBinariaLogica(node, node.getEsq(), node.getDir(), "or");
    }

    // Relacionais

    @Override
    public void outAMenorExpressao(AMenorExpressao node) {
        validaOperacaoRelacional(node, node.getEsq(), node.getDir(), "<");
    }

    @Override
    public void outAMaiorExpressao(AMaiorExpressao node) {
        validaOperacaoRelacional(node, node.getEsq(), node.getDir(), ">");
    }

    @Override
    public void outAIgualExpressao(AIgualExpressao node) {
        SymbolInfo.TypeKind tEsq = nodeTypes.get(node.getEsq());
        SymbolInfo.TypeKind tDir = nodeTypes.get(node.getDir());

        if (tEsq == SymbolInfo.TypeKind.ERRO || tDir == SymbolInfo.TypeKind.ERRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        } else if (tEsq == tDir) {
            nodeTypes.put(node, SymbolInfo.TypeKind.BOOLEANO);
        } else {
            reportError("Comparação de igualdade entre tipos incompatíveis (" + tEsq + " e " + tDir + ").", node);
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        }
    }

    @Override
    public void outATernarioExpressao(ATernarioExpressao node) {
        SymbolInfo.TypeKind cType = nodeTypes.get(node.getCond());
        SymbolInfo.TypeKind tType = nodeTypes.get(node.getVTrue());
        SymbolInfo.TypeKind fType = nodeTypes.get(node.getVFalse());

        if (cType != SymbolInfo.TypeKind.ERRO && cType != SymbolInfo.TypeKind.BOOLEANO) {
            reportError("Condição do operador ternário deve ser do tipo BOOLEANO, mas encontrou " + cType + ".", node.getCond());
        }

        if (tType == SymbolInfo.TypeKind.ERRO || fType == SymbolInfo.TypeKind.ERRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        } else if (tType == fType) {
            nodeTypes.put(node, tType);
        } else {
            reportError("Ramos do operador ternário possuem tipos incompatíveis (" + tType + " e " + fType + ").", node);
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        }
    }

    @Override
    public void outAAcessoExpressao(AAcessoExpressao node) {
        String attrName = node.getAtributo().getText().trim();
        SymbolInfo info = symbolTable.lookup(attrName);
        if (info != null) {
            nodeTypes.put(node, info.getTypeKind());
        } else {
            nodeTypes.put(node, SymbolInfo.TypeKind.INTEIRO);
        }
    }

    @Override
    public void outAChamadaExpressao(AChamadaExpressao node) {
        String methodName = node.getMetodo().getText().trim();
        if (methodName.equals("capture")) {
            nodeTypes.put(node, SymbolInfo.TypeKind.INTEIRO);
        } else {
            SymbolInfo info = symbolTable.lookup(methodName);
            if (info != null) {
                nodeTypes.put(node, info.getTypeKind());
            } else {
                nodeTypes.put(node, SymbolInfo.TypeKind.INTEIRO);
            }
        }
    }

    // Operações Auxiliares

    private void validaOperacaoBinariaAritmetica(Node node, PExpressao esq, PExpressao dir, String operacao) {
        SymbolInfo.TypeKind tEsq = nodeTypes.get(esq);
        SymbolInfo.TypeKind tDir = nodeTypes.get(dir);

        if (tEsq == SymbolInfo.TypeKind.ERRO || tDir == SymbolInfo.TypeKind.ERRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        } else if (tEsq == SymbolInfo.TypeKind.INTEIRO && tDir == SymbolInfo.TypeKind.INTEIRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.INTEIRO);
        } else {
            reportError("Operação de " + operacao + " inválida. Esperado (INTEIRO, INTEIRO), mas encontrado (" + tEsq + ", " + tDir + ").", node);
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        }
    }

    private void validaOperacaoBinariaLogica(Node node, PExpressao esq, PExpressao dir, String operacao) {
        SymbolInfo.TypeKind tEsq = nodeTypes.get(esq);
        SymbolInfo.TypeKind tDir = nodeTypes.get(dir);

        if (tEsq == SymbolInfo.TypeKind.ERRO || tDir == SymbolInfo.TypeKind.ERRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        } else if (tEsq == SymbolInfo.TypeKind.BOOLEANO && tDir == SymbolInfo.TypeKind.BOOLEANO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.BOOLEANO);
        } else {
            reportError("Operador lógico '" + operacao + "' exige operandos do tipo BOOLEANO, mas encontrou (" + tEsq + ", " + tDir + ").", node);
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        }
    }

    private void validaOperacaoRelacional(Node node, PExpressao esq, PExpressao dir, String op) {
        SymbolInfo.TypeKind tEsq = nodeTypes.get(esq);
        SymbolInfo.TypeKind tDir = nodeTypes.get(dir);

        if (tEsq == SymbolInfo.TypeKind.ERRO || tDir == SymbolInfo.TypeKind.ERRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        } else if (tEsq == SymbolInfo.TypeKind.INTEIRO && tDir == SymbolInfo.TypeKind.INTEIRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.BOOLEANO);
        } else {
            reportError("Operador relacional '" + op + "' exige operandos do tipo INTEIRO, mas encontrou (" + tEsq + ", " + tDir + ").", node);
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        }
    }

    // Atribuições

    @Override
    public void outAAtribuicaoComando(AAtribuicaoComando node) {
        String id = node.getId().getText().trim();
        int line = node.getId().getLine();
        int pos = node.getId().getPos();

        SymbolInfo info = symbolTable.lookup(id);

        if (info == null) {
            reportError("Identificador '" + id + "' não foi declarado.", line, pos);
            return;
        }

        // Proteção de Constantes (unalterable)
        if (info.getCategory() == SymbolInfo.Category.CONSTANT) {
            reportError("Tentativa de alterar o valor imutável da constante '" + id + "'.", line, pos);
        }

        // Validação de Atribuição (<<)
        SymbolInfo.TypeKind expType = nodeTypes.get(node.getExpressao());
        if (expType != null && expType != SymbolInfo.TypeKind.ERRO) {
            if (expType != info.getTypeKind()) {
                reportError("Atribuição incompatível para a variável '" + id + "'. Esperado tipo " + info.getTypeKind() + ", mas encontrado " + expType + ".", line, pos);
            }
        }
    }

    @Override
    public void outADecVar(ADecVar node) {
        SymbolInfo.TypeKind varType = determinePrimitivoType(node.getTipoPrimitivo());
        SymbolInfo.TypeKind expType = nodeTypes.get(node.getExpressao());

        if (expType != null && expType != SymbolInfo.TypeKind.ERRO && expType != varType) {
            reportError("Inicialização incompatível para a variável '" + node.getId().getText().trim() + "'. Esperado " + varType + ", mas encontrado " + expType + ".", node.getId().getLine(), node.getId().getPos());
        }
    }

    @Override
    public void outADecCons(ADecCons node) {
        SymbolInfo.TypeKind consType = determinePrimitivoType(node.getTipoPrimitivo());
        SymbolInfo.TypeKind expType = nodeTypes.get(node.getExpressao());

        if (expType != null && expType != SymbolInfo.TypeKind.ERRO && expType != consType) {
            reportError("Inicialização incompatível para a constante '" + node.getId().getText().trim() + "'. Esperado " + consType + ", mas encontrado " + expType + ".", node.getId().getLine(), node.getId().getPos());
        }
    }

    @Override
    public void outACondicionalComando(ACondicionalComando node) {
        PExpressao condExpr = node.getExpressao();
        SymbolInfo.TypeKind condType = nodeTypes.get(condExpr);

        if (condType != null && condType != SymbolInfo.TypeKind.ERRO && condType != SymbolInfo.TypeKind.BOOLEANO) {
            reportError("A condição do comando condicional (if) deve ser do tipo BOOLEANO, mas foi encontrado " + condType + ".", condExpr);
        }
    }

    @Override
    public void outALacoComando(ALacoComando node) {
        PExpressao condExpr = node.getExpressao();
        SymbolInfo.TypeKind condType = nodeTypes.get(condExpr);

        if (condType != null && condType != SymbolInfo.TypeKind.ERRO && condType != SymbolInfo.TypeKind.BOOLEANO) {
            reportError("A condição do laço de repetição (while / as long as) deve ser do tipo BOOLEANO, mas foi encontrado " + condType + ".", condExpr);
        }
    }

    // Gerenciamento de Escopos e Declarações

    @Override
    public void inADeclaracaoClasse(ADeclaracaoClasse node) {
        String className = node.getIdClasse().getText().trim();
        symbolTable.declare(className, new SymbolInfo(className, SymbolInfo.Category.CLASS, SymbolInfo.TypeKind.CLASSE));
        symbolTable.enterScope();
    }

    @Override
    public void outADeclaracaoClasse(ADeclaracaoClasse node) {
        symbolTable.exitScope();
    }

    @Override
    public void inAFuncaoConcretaMetodo(AFuncaoConcretaMetodo node) {
        String name = node.getId().getText().trim();
        SymbolInfo.TypeKind retType = determineTipo(node.getTipo());
        symbolTable.declare(name, new SymbolInfo(name, SymbolInfo.Category.METHOD, retType));
        symbolTable.enterScope();
    }

    @Override
    public void outAFuncaoConcretaMetodo(AFuncaoConcretaMetodo node) {
        symbolTable.exitScope();
    }

    @Override
    public void inAProcedimentoConcrMetodo(AProcedimentoConcrMetodo node) {
        String name = node.getId().getText().trim();
        symbolTable.declare(name, new SymbolInfo(name, SymbolInfo.Category.METHOD, SymbolInfo.TypeKind.CLASSE));
        symbolTable.enterScope();
    }

    @Override
    public void outAProcedimentoConcrMetodo(AProcedimentoConcrMetodo node) {
        symbolTable.exitScope();
    }

    @Override
    public void inABlocoComandos(ABlocoComandos node) {
        symbolTable.enterScope();
    }

    @Override
    public void outABlocoComandos(ABlocoComandos node) {
        symbolTable.exitScope();
    }

    @Override
    public void inABlocoExp(ABlocoExp node) {
        symbolTable.enterScope();
    }

    @Override
    public void outABlocoExp(ABlocoExp node) {
        symbolTable.exitScope();
    }

    @Override
    public void inADecVar(ADecVar node) {
        String id = node.getId().getText().trim();
        SymbolInfo.TypeKind type = determinePrimitivoType(node.getTipoPrimitivo());

        SymbolInfo info = new SymbolInfo(id, SymbolInfo.Category.VARIABLE, type);
        if (!symbolTable.declare(id, info)) {
            reportError("Variável '" + id + "' já foi declarada neste escopo.", node.getId().getLine(), node.getId().getPos());
        }
    }

    @Override
    public void inADecCons(ADecCons node) {
        String id = node.getId().getText().trim();
        SymbolInfo.TypeKind type = determinePrimitivoType(node.getTipoPrimitivo());

        SymbolInfo info = new SymbolInfo(id, SymbolInfo.Category.CONSTANT, type);
        if (!symbolTable.declare(id, info)) {
            reportError("Constante '" + id + "' já foi declarada neste escopo.", node.getId().getLine(), node.getId().getPos());
        }
    }

    @Override
    public void inADecObj(ADecObj node) {
        String id = node.getId().getText().trim();
        String targetClass = node.getIdClasse().getText().trim();

        if (symbolTable.lookup(targetClass) == null) {
            reportError("Tipo de classe '" + targetClass + "' não foi definido.", node.getId().getLine(), node.getId().getPos());
        }

        SymbolInfo info = new SymbolInfo(id, SymbolInfo.Category.OBJECT, targetClass);
        if (!symbolTable.declare(id, info)) {
            reportError("Objeto '" + id + "' já foi declarado neste escopo.", node.getId().getLine(), node.getId().getPos());
        }
    }

    @Override
    public void inAParametro(AParametro node) {
        String id = node.getId().getText().trim();
        SymbolInfo.TypeKind type = determineTipo(node.getTipo());

        SymbolInfo info = new SymbolInfo(id, SymbolInfo.Category.VARIABLE, type);
        if (!symbolTable.declare(id, info)) {
            reportError("Parâmetro '" + id + "' já foi declarado neste escopo.", node.getId().getLine(), node.getId().getPos());
        }
    }

    // Helper methods para conversão de tipos
    private SymbolInfo.TypeKind determinePrimitivoType(PTipoPrimitivo tipoNode) {
        if (tipoNode instanceof AInteiroTipoPrimitivo) {
            return SymbolInfo.TypeKind.INTEIRO;
        }
        return SymbolInfo.TypeKind.BOOLEANO;
    }

    private SymbolInfo.TypeKind determineTipo(PTipo tipoNode) {
        if (tipoNode instanceof APrimitivoTipo) {
            return determinePrimitivoType(((APrimitivoTipo) tipoNode).getTipoPrimitivo());
        }
        return SymbolInfo.TypeKind.CLASSE;
    }
}
