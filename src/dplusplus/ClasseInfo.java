package dplusplus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Informacoes de uma classe coletadas na primeira passada: nome, classe mae
 * (para o grafo de heranca), metodos e atributos declarados.
 */
public class ClasseInfo {
    public final String nome;
    public String pai;                                       // "Root" por padrao
    public final Map<String, MetodoInfo> metodos = new LinkedHashMap<String, MetodoInfo>();
    public final Map<String, Tipo> atributos     = new LinkedHashMap<String, Tipo>();
    public boolean nativa = false;                           // ex.: Periphericals

    public int linha = 0;                                    // posicao da declaracao (p/ erros)
    public int coluna = 0;

    public ClasseInfo(String nome) {
        this.nome = nome;
        this.pai = "Root";
    }
}
