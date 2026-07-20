package dplusplus;

/**
 * Tipo semantico da linguagem D++.
 *
 * Cobre os primitivos (number/answer), tipos de classe e marcadores auxiliares
 * usados pela analise (VOID para procedimentos, QUALQUER para os metodos nativos
 * de Periphericals, ERRO para propagar falhas sem gerar erros em cascata).
 */
public class Tipo {
    public enum Kind { INTEIRO, BOOLEANO, CLASSE, VOID, QUALQUER, ERRO }

    public final Kind kind;
    public final String classe; // preenchido apenas quando kind == CLASSE

    private Tipo(Kind kind, String classe) {
        this.kind = kind;
        this.classe = classe;
    }

    public static final Tipo INTEIRO  = new Tipo(Kind.INTEIRO, null);
    public static final Tipo BOOLEANO = new Tipo(Kind.BOOLEANO, null);
    public static final Tipo VOID     = new Tipo(Kind.VOID, null);
    public static final Tipo QUALQUER = new Tipo(Kind.QUALQUER, null);
    public static final Tipo ERRO     = new Tipo(Kind.ERRO, null);

    public static Tipo classe(String nome) {
        return new Tipo(Kind.CLASSE, nome);
    }

    public boolean isErro() { return kind == Kind.ERRO; }

    @Override
    public String toString() {
        switch (kind) {
            case INTEIRO:  return "number";
            case BOOLEANO: return "answer";
            case CLASSE:   return classe;
            case VOID:     return "void";
            case QUALQUER: return "qualquer";
            default:       return "<erro>";
        }
    }
}
