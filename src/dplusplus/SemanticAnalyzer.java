package dplusplus;

import dplusplus.analysis.DepthFirstAdapter;
import dplusplus.node.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * Analise semantica - Tarefa 3: Metodos, Parametros e Grafo de Classes.
 *
 * Implementa tres verificacoes, em duas passadas sobre a AST:
 *   1. Grafo de heranca: a classe mae existe e nao ha ciclos de heranca.
 *   2. Validacao de retorno: o tipo da expressao final de uma funcao bate
 *      com o tipo de retorno declarado na assinatura.
 *   3. Casamento de parametros: numa chamada, confere se o metodo existe e se
 *      a quantidade e os tipos dos argumentos batem com a assinatura, na ordem.
 *
 * Passada A (coletar): registra classes, atributos, assinaturas de metodos e
 * as relacoes de heranca - sem validar corpos, para permitir chamadas e
 * herancas com referencia a entidades declaradas em qualquer ponto.
 * Passada B (apply/DepthFirst): valida retorno e chamadas dentro dos corpos.
 */


public class SemanticAnalyzer extends DepthFirstAdapter {

    // --- ESTRUTURAS DA TAREFA 3 (Herança, Classes e Métodos) ---
    private static final class Rel {
        final TIdClasse filho;
        final TIdClasse pai;
        Rel(TIdClasse filho, TIdClasse pai) { this.filho = filho; this.pai = pai; }
    }

    private final Map<String, ClasseInfo> classes = new LinkedHashMap<String, ClasseInfo>();
    private final Map<String, String> heranca = new HashMap<String, String>(); // filho -> pai
    private final List<Rel> relacoes = new ArrayList<Rel>();

    // --- ESTRUTURAS DE SUPORTE (Tabela de Símbolos, Tipos e Erros) ---
    private SymbolTable symbolTable;
    private final Map<Node, SymbolInfo.TypeKind> nodeTypes = new HashMap<>();
    private final List<String> errorList = new ArrayList<>();
    private final List<String> erros = errorList; // Atalho de compatibilidade para a tarefa-3

    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
    }

    // --- MÉTODOS AUXILIARES DE TRATAMENTO DE ERROS ---
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

    public List<String> getErrorList() {
        return errorList;
    }

    // Pilha de escopos de variaveis locais/parametros (nome -> tipo) na passada B.
    private final Deque<Map<String, Tipo>> escopos = new ArrayDeque<Map<String, Tipo>>();
    private ClasseInfo classeAtual;
    private int profundidadeMetodo = 0;

    // ---------------------------------------------------------------- API

    public boolean temErros() { return !erros.isEmpty(); }
    public List<String> getErros() { return erros; }

    // Aliases (compatibilidade com o Main que usa hasErrors()/printErrors()).
    public void printErrors() {
        for (String e : erros) System.err.println("  -> " + e);
    }

    /** Ponto de entrada explicito: roda a analise completa. */
    public void analisar(Start ast) {
        ast.apply(this);      // dispara inStart (passadas A/heranca) + passada B
    }

    /**
     * Passada A (coleta) + Feature 1 (heranca), executadas antes de a passada B
     * percorrer a arvore. Fica em inStart para funcionar mesmo quando o Main
     * chama apenas ast.apply(analisador).
     */
    @Override
    public void inStart(Start node) {
        registrarNativas();
        APrograma prog = (APrograma) node.getPPrograma();
        coletar(prog);        // Passada A: classes, atributos, assinaturas, heranca
        validarHeranca();     // Feature 1: grafo de heranca (pai existe + ciclos)
    }

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

    // ------------------------------------------------- classes nativas

    private void registrarNativas() {
        ClasseInfo p = new ClasseInfo("Periphericals");
        p.nativa = true;
        // show[exp] : procedimento que imprime; aceita qualquer primitivo.
        List<Tipo> paramShow = new ArrayList<Tipo>();
        paramShow.add(Tipo.QUALQUER);
        p.metodos.put("show", new MetodoInfo("show", Tipo.VOID, paramShow, false, false));
        // capture[] : funcao que le do teclado; retorna primitivo (compativel c/ qualquer).
        p.metodos.put("capture", new MetodoInfo("capture", Tipo.QUALQUER, new ArrayList<Tipo>(), false, false));
        classes.put("Periphericals", p);
    }

    // =================================================== PASSADA A: coleta

    private void coletar(APrograma prog) {
        if (prog.getGenealogia() != null) {
            AGenealogia g = (AGenealogia) prog.getGenealogia();
            for (PRelacao pr : g.getRelacao()) {
                ARelacao r = (ARelacao) pr;
                String filho = r.getFilho().getText().trim();
                String pai = r.getPai().getText().trim();
                relacoes.add(new Rel(r.getFilho(), r.getPai()));
                if (heranca.containsKey(filho) && !heranca.get(filho).equals(pai)) {
                    reportError("Classe '" + filho + "' nao pode ter mais de uma classe mae.",
                            r.getFilho().getLine(), r.getFilho().getPos());
                } else {
                    heranca.put(filho, pai);
                }
            }
        }

        for (PDeclaracaoClasse pd : prog.getDeclaracaoClasse()) {
            ADeclaracaoClasse dc = (ADeclaracaoClasse) pd;
            String nome = dc.getIdClasse().getText().trim();
            if (classes.containsKey(nome)) {
                reportError("Classe '" + nome + "' declarada mais de uma vez.",
                        dc.getIdClasse().getLine(), dc.getIdClasse().getPos());
                continue;
            }
            ClasseInfo ci = new ClasseInfo(nome);
            ci.linha = dc.getIdClasse().getLine();
            ci.coluna = dc.getIdClasse().getPos();
            classes.put(nome, ci);
            for (PComponentes pc : dc.getComponentes()) {
                coletarComponente(ci, pc);
            }
        }

        // Aplica a heranca coletada aos ClasseInfo ja registrados.
        for (Map.Entry<String, String> e : heranca.entrySet()) {
            ClasseInfo ci = classes.get(e.getKey());
            if (ci != null) ci.pai = e.getValue();
        }
    }

    private void coletarComponente(ClasseInfo ci, PComponentes pc) {
        if (pc instanceof AVarComponentes) {
            ADecVar dv = (ADecVar) ((AVarComponentes) pc).getDecVar();
            ci.atributos.put(dv.getId().getText().trim(), tipoPrimitivo(dv.getTipoPrimitivo()));
        } else if (pc instanceof AConsComponentes) {
            ADecCons dcs = (ADecCons) ((AConsComponentes) pc).getDecCons();
            ci.atributos.put(dcs.getId().getText().trim(), tipoPrimitivo(dcs.getTipoPrimitivo()));
        } else if (pc instanceof AObjComponentes) {
            ADecObj dob = (ADecObj) ((AObjComponentes) pc).getDecObj();
            ci.atributos.put(dob.getId().getText().trim(),
                    Tipo.classe(dob.getIdClasse().getText().trim()));
        } else if (pc instanceof AMetodoComponentes) {
            coletarMetodo(ci, ((AMetodoComponentes) pc).getMetodo());
        }
    }

    private void coletarMetodo(ClasseInfo ci, PMetodo m) {
        String nome;
        MetodoInfo info;
        if (m instanceof AFuncaoConcretaMetodo) {
            AFuncaoConcretaMetodo f = (AFuncaoConcretaMetodo) m;
            nome = f.getId().getText().trim();
            info = new MetodoInfo(nome, tipoDeclarado(f.getTipo()), tiposParams(f.getParametro()), false, false);
        } else if (m instanceof AFuncaoAbstrataMetodo) {
            AFuncaoAbstrataMetodo f = (AFuncaoAbstrataMetodo) m;
            nome = f.getId().getText().trim();
            info = new MetodoInfo(nome, tipoDeclarado(f.getTipo()), tiposParams(f.getParametro()), true, false);
        } else if (m instanceof AProcedimentoConcrMetodo) {
            AProcedimentoConcrMetodo p = (AProcedimentoConcrMetodo) m;
            nome = p.getId().getText().trim();
            info = new MetodoInfo(nome, Tipo.VOID, tiposParams(p.getParametro()), false, p.getMarcador() != null);
        } else {
            AProcedimentoAbstMetodo p = (AProcedimentoAbstMetodo) m;
            nome = p.getId().getText().trim();
            info = new MetodoInfo(nome, Tipo.VOID, tiposParams(p.getParametro()), true, p.getMarcador() != null);
        }
        if (ci.metodos.containsKey(nome)) {
            reportError("Metodo '" + nome + "' declarado mais de uma vez na classe '" + ci.nome + "'.",
                    ci.linha, ci.coluna);
        } else {
            ci.metodos.put(nome, info);
        }
    }

    private List<Tipo> tiposParams(LinkedList<PParametro> params) {
        List<Tipo> tipos = new ArrayList<Tipo>();
        for (PParametro pp : params) {
            AParametro p = (AParametro) pp;
            tipos.add(tipoDeclarado(p.getTipo()));
        }
        return tipos;
    }

    // ============================================ Feature 1: grafo de heranca

    private void validarHeranca() {
        // (a) a classe mae de cada relacao existe?
        for (Rel r : relacoes) {
            String filho = r.filho.getText().trim();
            String pai = r.pai.getText().trim();
            if (!pai.equals("Root") && !classes.containsKey(pai)) {
                reportError("Classe mae '" + pai + "' (de '" + filho + "') nao foi declarada.",
                        r.pai.getLine(), r.pai.getPos());
            }
            if (!classes.containsKey(filho)) {
                reportError("Classe '" + filho + "' aparece na genealogia mas nao foi declarada.",
                        r.filho.getLine(), r.filho.getPos());
            }
        }

        // (b) deteccao de ciclos, subindo pela cadeia de pais.
        Set<String> reportadas = new HashSet<String>();
        for (String inicio : classes.keySet()) {
            if (reportadas.contains(inicio)) continue;
            Set<String> visto = new HashSet<String>();
            List<String> caminho = new ArrayList<String>();
            String atual = inicio;
            while (atual != null && !atual.equals("Root") && classes.containsKey(atual)) {
                if (visto.contains(atual)) {
                    caminho.add(atual); // fecha o ciclo visualmente
                    ClasseInfo ci = classes.get(inicio);
                    reportError("Heranca ciclica detectada: " + juntar(caminho, atual),
                            ci.linha, ci.coluna);
                    reportadas.addAll(visto);
                    break;
                }
                visto.add(atual);
                caminho.add(atual);
                atual = classes.get(atual).pai;
            }
        }
    }

    private String juntar(List<String> caminho, String fecha) {
        StringBuilder sb = new StringBuilder();
        boolean pulou = false;
        for (int i = 0; i < caminho.size(); i++) {
            // corta o prefixo antes do inicio do ciclo
            if (!pulou && caminho.get(i).equals(fecha) && i < caminho.size() - 1) pulou = true;
            if (i > 0) sb.append(" -> ");
            sb.append(caminho.get(i));
        }
        return sb.toString();
    }

    // ========================================== PASSADA B: retorno e chamadas
    // (via DepthFirstAdapter: os hooks abaixo sao chamados durante ast.apply)

    @Override
    public void inADeclaracaoClasse(ADeclaracaoClasse node) {
        classeAtual = classes.get(node.getIdClasse().getText().trim());
        abrirEscopo();
    }

    @Override
    public void outADeclaracaoClasse(ADeclaracaoClasse node) {
        fecharEscopo();
        classeAtual = null;
    }

    @Override
    public void inAFuncaoConcretaMetodo(AFuncaoConcretaMetodo node) {
        profundidadeMetodo++;
        abrirEscopo();
        declararParametros(node.getParametro());
    }

    @Override
    public void outAFuncaoConcretaMetodo(AFuncaoConcretaMetodo node) {
        // Feature 2: validacao de retorno (escopo ainda aberto -> locais visiveis).
        Tipo declarado = tipoDeclarado(node.getTipo());
        ABlocoExp corpo = (ABlocoExp) node.getBlocoExp();
        Tipo real = tipoDe(corpo.getExpressao());
        if (!compativel(declarado, real)) {
            reportError("Tipo de retorno incompativel na funcao '" + node.getId().getText().trim()
                    + "': declarado " + declarado + ", mas a expressao final e " + real + ".",
                    node.getId().getLine(), node.getId().getPos());
        }
        fecharEscopo();
        profundidadeMetodo--;
    }

    @Override
    public void inAProcedimentoConcrMetodo(AProcedimentoConcrMetodo node) {
        profundidadeMetodo++;
        abrirEscopo();
        declararParametros(node.getParametro());
    }

    @Override
    public void outAProcedimentoConcrMetodo(AProcedimentoConcrMetodo node) {
        fecharEscopo();
        profundidadeMetodo--;
    }

    // Blocos start/finish aninhados (dentro de comandos) abrem sub-escopo.
    @Override
    public void inABlocoComandos(ABlocoComandos node) {
        if (profundidadeMetodo > 0) abrirEscopo();
    }

    @Override
    public void outABlocoComandos(ABlocoComandos node) {
        if (profundidadeMetodo > 0) fecharEscopo();
    }

    // Declaracoes locais (dentro de metodos) entram no escopo corrente.
    @Override
    public void inADecVar(ADecVar node) {
        if (profundidadeMetodo > 0)
            declararLocal(node.getId().getText().trim(), tipoPrimitivo(node.getTipoPrimitivo()));
    }

    @Override
    public void inADecCons(ADecCons node) {
        if (profundidadeMetodo > 0)
            declararLocal(node.getId().getText().trim(), tipoPrimitivo(node.getTipoPrimitivo()));
    }

    @Override
    public void inADecObj(ADecObj node) {
        if (profundidadeMetodo > 0)
            declararLocal(node.getId().getText().trim(),
                    Tipo.classe(node.getIdClasse().getText().trim()));
    }

    // Feature 3: casamento de parametros nas chamadas.
    @Override
    public void inAChamadaExpressao(AChamadaExpressao node) {
        validarChamada(node.getPrefixos(), node.getMetodo(), node.getExpressao());
    }

    @Override
    public void inAChamadaCmdComando(AChamadaCmdComando node) {
        validarChamada(node.getPrefixos(), node.getMetodo(), node.getExpressao());
    }

    private void validarChamada(LinkedList<TId> prefixos, TId metodoTok, LinkedList<PExpressao> args) {
        String receptor = resolverReceptor(prefixos);
        if (receptor == null) return; // objeto receptor nao resolvido -> tratado por outra etapa
        String nome = metodoTok.getText().trim();
        MetodoInfo m = buscarMetodo(receptor, nome);
        if (m == null) {
            reportError("Metodo '" + nome + "' nao encontrado na classe '" + receptor
                    + "' nem em seus ancestrais.", metodoTok.getLine(), metodoTok.getPos());
            return;
        }
        if (args.size() != m.parametros.size()) {
            reportError("Metodo '" + nome + "' espera " + m.parametros.size()
                    + " argumento(s), mas recebeu " + args.size() + ".",
                    metodoTok.getLine(), metodoTok.getPos());
            return;
        }
        for (int i = 0; i < args.size(); i++) {
            Tipo esperado = m.parametros.get(i);
            Tipo atual = tipoDe(args.get(i));
            if (!compativel(esperado, atual)) {
                reportError("Argumento " + (i + 1) + " do metodo '" + nome + "': esperado "
                        + esperado + ", recebido " + atual + ".",
                        metodoTok.getLine(), metodoTok.getPos());
            }
        }
    }

    // ------------------------------------------------ resolucao de receptor

    /** Classe do objeto receptor de uma chamada, ou null se nao resolvivel. */
    private String resolverReceptor(LinkedList<TId> prefixos) {
        if (prefixos.isEmpty()) {
            return classeAtual != null ? classeAtual.nome : null;
        }
        Tipo t = tipoDeId(prefixos.get(0).getText().trim());
        for (int i = 1; i < prefixos.size() && t.kind == Tipo.Kind.CLASSE; i++) {
            t = buscarAtributo(t.classe, prefixos.get(i).getText().trim());
        }
        return t.kind == Tipo.Kind.CLASSE ? t.classe : null;
    }

    // ----------------------------------------- resolvedor de tipo de expressao

    private Tipo tipoDe(PExpressao e) {
        if (e instanceof AInteiroExpressao || e instanceof ARealExpressao) return Tipo.INTEIRO;
        if (e instanceof AVerdadeiroExpressao || e instanceof AFalsoExpressao) return Tipo.BOOLEANO;
        if (e instanceof ANegacaoExpressao) return Tipo.BOOLEANO;
        if (e instanceof AMenosUnarioExpressao) return tipoDe(((AMenosUnarioExpressao) e).getExpressao());
        if (e instanceof ASomaExpressao || e instanceof ASubtracaoExpressao
                || e instanceof AMultExpressao || e instanceof ADivExpressao) return Tipo.INTEIRO;
        if (e instanceof AMenorExpressao || e instanceof AMaiorExpressao
                || e instanceof AIgualExpressao) return Tipo.BOOLEANO;
        if (e instanceof AEExpressao || e instanceof AOuExpressao) return Tipo.BOOLEANO;
        if (e instanceof AIdExpressao) {
            return tipoDeId(((AIdExpressao) e).getId().getText().trim());
        }
        if (e instanceof ATernarioExpressao) {
            return tipoDe(((ATernarioExpressao) e).getVTrue());
        }
        if (e instanceof AAcessoExpressao) {
            AAcessoExpressao ac = (AAcessoExpressao) e;
            Tipo t = tipoDeId(ac.getPrefixos().get(0).getText().trim());
            for (int i = 1; i < ac.getPrefixos().size() && t.kind == Tipo.Kind.CLASSE; i++) {
                t = buscarAtributo(t.classe, ac.getPrefixos().get(i).getText().trim());
            }
            if (t.kind != Tipo.Kind.CLASSE) return Tipo.ERRO;
            return buscarAtributo(t.classe, ac.getAtributo().getText().trim());
        }
        if (e instanceof AChamadaExpressao) {
            AChamadaExpressao ch = (AChamadaExpressao) e;
            String receptor = resolverReceptor(ch.getPrefixos());
            if (receptor == null) return Tipo.ERRO;
            MetodoInfo m = buscarMetodo(receptor, ch.getMetodo().getText().trim());
            return m != null ? m.retorno : Tipo.ERRO;
        }
        return Tipo.ERRO;
    }

    private Tipo tipoDeId(String id) {
        Tipo local = buscarLocal(id);
        if (local != null) return local;
        if (classeAtual != null) {
            Tipo attr = buscarAtributo(classeAtual.nome, id);
            if (!attr.isErro()) return attr;
        }
        return Tipo.ERRO;
    }

    // ------------------------------------------- busca com heranca

    private MetodoInfo buscarMetodo(String classe, String metodo) {
        String c = classe;
        int guarda = 0;
        while (c != null && !c.equals("Root") && guarda++ < 100) {
            ClasseInfo ci = classes.get(c);
            if (ci == null) return null;
            if (ci.metodos.containsKey(metodo)) return ci.metodos.get(metodo);
            c = ci.pai;
        }
        return null;
    }

    private Tipo buscarAtributo(String classe, String attr) {
        String c = classe;
        int guarda = 0;
        while (c != null && !c.equals("Root") && guarda++ < 100) {
            ClasseInfo ci = classes.get(c);
            if (ci == null) return Tipo.ERRO;
            if (ci.atributos.containsKey(attr)) return ci.atributos.get(attr);
            c = ci.pai;
        }
        return Tipo.ERRO;
    }

    /** atual e compativel com esperado (inclui subtipagem por heranca). */
    private boolean compativel(Tipo esperado, Tipo atual) {
        if (esperado.isErro() || atual.isErro()) return true; // evita cascata
        if (esperado.kind == Tipo.Kind.QUALQUER || atual.kind == Tipo.Kind.QUALQUER) return true;
        if (esperado.kind == Tipo.Kind.CLASSE && atual.kind == Tipo.Kind.CLASSE) {
            return ehSubclasse(atual.classe, esperado.classe);
        }
        return esperado.kind == atual.kind;
    }

    private boolean ehSubclasse(String filho, String ancestral) {
        String c = filho;
        int guarda = 0;
        while (c != null && guarda++ < 100) {
            if (c.equals(ancestral)) return true;
            if (c.equals("Root")) return false;
            ClasseInfo ci = classes.get(c);
            if (ci == null) return false;
            c = ci.pai;
        }
        return false;
    }

    // ------------------------------------------------- escopos e tipos

    private void abrirEscopo() { escopos.push(new HashMap<String, Tipo>()); }
    private void fecharEscopo() { if (!escopos.isEmpty()) escopos.pop(); }
    private void declararLocal(String nome, Tipo t) {
        if (!escopos.isEmpty()) escopos.peek().put(nome, t);
    }
    private Tipo buscarLocal(String nome) {
        for (Map<String, Tipo> escopo : escopos) {
            if (escopo.containsKey(nome)) return escopo.get(nome);
        }
        return null;
    }

    private void declararParametros(LinkedList<PParametro> params) {
        for (PParametro pp : params) {
            AParametro p = (AParametro) pp;
            declararLocal(p.getId().getText().trim(), tipoDeclarado(p.getTipo()));
        }
    }

    private Tipo tipoDeclarado(PTipo t) {
        if (t instanceof APrimitivoTipo) {
            return tipoPrimitivo(((APrimitivoTipo) t).getTipoPrimitivo());
        }
        return Tipo.classe(((AClasseTipo) t).getIdClasse().getText().trim());
    }

    private Tipo tipoPrimitivo(PTipoPrimitivo tp) {
        return (tp instanceof AInteiroTipoPrimitivo) ? Tipo.INTEIRO : Tipo.BOOLEANO;
    }


}
