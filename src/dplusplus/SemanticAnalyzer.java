package dplusplus;
import dplusplus.analysis.DepthFirstAdapter;
import dplusplus.node.*; // Nós gerados automaticamente pela AST
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class SemanticAnalyzer extends DepthFirstAdapter {
    private SymbolTable symbolTable;
    private List<String> errorList; // Acumulador de mensagens de erro

    // Cria um dicionário que associa objetos Node a às caractegorias de tipos
    private final Map<Node, SymbolInfo.TypeKind> nodeTypes = new HashMap<>();
    
    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.errorList = new ArrayList<>();
    }
    
    // Regista um novo erro sem interromper o caminhamento da árvore
    public void reportError(String message, int line, int pos) {
        errorList.add("Erro Semântico [" + line + "," + pos + "]: " + message);
    }

    public boolean hasErrors() {
        return !errorList.isEmpty();
    }

    public void printErrors() {
        for (String error : errorList) {
            System.err.println("  -> " + error);
        }
    }

    // METODOS OUT
    @Override
    public void outANumeroLiteral(ANumeroLiteral node) {
        nodeTypes.put(node, SymbolInfo.TypeKind.INTEIRO);
    }


    @Override
    public void outAIdExpressao(AIdExpressao node) {
        String id = node.getId().getText().trim();
        SymbolInfo info = symbolTable.lookup(id);

        if (info == null) {
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        } else {
            nodeTypes.put(node, info.getType());
        }
    }

    // Operação: Soma
    @Override
    public void outASomaExpressao(ASomaExpressao node) {
        Node esquerdo = node.getEsquerda();
        Node direito = node.getDireita();

        SymbolInfo.TypeKind tipoEsq = nodeTypes.get(esquerdo);
        SymbolInfo.TypeKind tipoDir = nodeTypes.get(direito);

        if (tipoEsq == SymbolInfo.TypeKind.ERRO || tipoDir == SymbolInfo.TypeKind.ERRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
            return;
        }

        if (tipoEsq == SymbolInfo.TypeKind.INTEIRO && tipoDir == SymbolInfo.TypeKind.INTEIRO) {
            nodeTypes.put(node, SymbolInfo.TypeKind.INTEIRO);
        } else {
            int linha = node.getMais().getLine();
            int coluna = node.getMais().getPos();
            
            reportError("Operação de soma inválida. Esperado (INTEIRO, INTEIRO) mas encontrado (" + tipoEsq + ", " + tipoDir + ").", linha, coluna);
            nodeTypes.put(node, SymbolInfo.TypeKind.ERRO);
        }
    }
    // --- GERENCIAMENTO DE ESCOPOS ---

    @Override
    public void inADeclaracaoClasse(ADeclaracaoClasse node) {
        // 1. Registra a classe no escopo atual (Global)
        String className = node.getIdClasse().getText().trim();
        symbolTable.declare(className, new SymbolInfo(className, SymbolInfo.Category.CLASS, SymbolInfo.TypeKind.CLASSE));
        
        // 2. Entra no escopo interno da classe (para guardar atributos e métodos)
        symbolTable.enterScope();
    }

    @Override
    public void outADeclaracaoClasse(ADeclaracaoClasse node) {
        symbolTable.exitScope(); // Sai do escopo da classe
    }

    @Override
    public void inABlocoComandos(ABlocoComandos node) {
        symbolTable.enterScope(); // Blocos start/finish locais abrem novo escopo
    }

    @Override
    public void outABlocoComandos(ABlocoComandos node) {
        symbolTable.exitScope();
    }

    // --- VALIDAÇÃO DE DECLARAÇÕES ---

    @Override
    public void inADecVar(ADecVar node) {
        String id = node.getId().getText().trim();
        SymbolInfo.TypeKind type = determinePrimitivoType(node.getTipoPrimitivo());

        SymbolInfo info = new SymbolInfo(id, SymbolInfo.Category.VARIABLE, type);
        if (!symbolTable.declare(id, info)) {
            System.err.println("Erro Semântico: Variável '" + id + "' já declarada neste escopo.");
            // Aqui você pode incrementar um contador de erros para abortar a compilação depois
        }
    }

    @Override
    public void inADecCons(ADecCons node) {
        String id = node.getId().getText().trim();
        SymbolInfo.TypeKind type = determinePrimitivoType(node.getTipoPrimitivo());

        SymbolInfo info = new SymbolInfo(id, SymbolInfo.Category.CONSTANT, type);
        if (!symbolTable.declare(id, info)) {
            System.err.println("Erro Semântico: Constante '" + id + "' já declarada neste escopo.");
        }
    }

    @Override
    public void inADecObj(ADecObj node) {
        String id = node.getId().getText().trim();
        String targetClass = node.getIdClasse().getText().trim();

        // Regra Semântica Extra: A classe do objeto existe?
        if (symbolTable.lookup(targetClass) == null) {
            System.err.println("Erro Semântico: Tipo de classe '" + targetClass + "' não foi definido.");
        }

        SymbolInfo info = new SymbolInfo(id, SymbolInfo.Category.OBJECT, targetClass);
        if (!symbolTable.declare(id, info)) {
            System.err.println("Erro Semântico: Objeto '" + id + "' já declarado neste escopo.");
        }
    }

    // --- VALIDAÇÃO DE USO (LOOKUP) ---

    @Override
    public void inAIdExpressao(AIdExpressao node) {
        String id = node.getId().getText().trim();
        SymbolInfo info = symbolTable.lookup(id);

        if (info == null) {
            System.err.println("Erro Semântico: Identificador '" + id + "' não foi declarado.");
        }
    }

    // Método auxiliar para converter nós da AST em ENUM de tipos
    private SymbolInfo.TypeKind determinePrimitivoType(PTipoPrimitivo tipoNode) {
        if (tipoNode instanceof AInteiroTipoPrimitivo) {
            return SymbolInfo.TypeKind.INTEIRO;
        }
        return SymbolInfo.TypeKind.BOOLEANO;
    }
}


