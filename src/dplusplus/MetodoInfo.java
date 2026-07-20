package dplusplus;

import java.util.ArrayList;
import java.util.List;

/**
 * Assinatura de um metodo (funcao ou procedimento) registrada na tabela.
 * Usada para o casamento de parametros e a validacao de retorno.
 */
public class MetodoInfo {
    public final String nome;
    public final Tipo retorno;          // Tipo.VOID para procedimentos
    public final List<Tipo> parametros; // tipos dos parametros, na ordem
    public final boolean abstrato;      // metodo sem corpo (apenas assinatura)
    public final boolean pontoEntrada;  // procedimento marcado com '>>'

    public MetodoInfo(String nome, Tipo retorno, List<Tipo> parametros,
                      boolean abstrato, boolean pontoEntrada) {
        this.nome = nome;
        this.retorno = retorno;
        this.parametros = parametros != null ? parametros : new ArrayList<Tipo>();
        this.abstrato = abstrato;
        this.pontoEntrada = pontoEntrada;
    }
}
