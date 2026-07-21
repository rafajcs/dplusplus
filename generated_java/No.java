import java.util.Scanner;

public class No extends NoBase {
    No proximo = null;
    double valor = 0;
    public void setValor(double v) {
        valor = v;
    }
    public double getValor() {
        return valor;
    }
    public void setProximo(No n) {
        proximo = n;
    }
}
