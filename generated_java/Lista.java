import java.util.Scanner;

public class Lista {
    No cabeca = null;
    double tamanho = 0;
    public void inserir(double v) {
        No novo = new No();
        novo.setValor(v);
        novo.setProximo(cabeca);
        cabeca = novo;
        tamanho = tamanho + 1;
    }
    public double getTamanho() {
        return tamanho;
    }
}
