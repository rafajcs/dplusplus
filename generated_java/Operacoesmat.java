import java.util.Scanner;

public class Operacoesmat {
    public static double calcula_fatorial() {
        double n = new java.util.Scanner(System.in).nextDouble();
        double resultado = 1;
        if (n < 0) {
            System.out.println(-10000000000);
        }
        if (n > 1) {
            while (n > 1) {
                resultado = resultado * n;
                n = n - 1;
            }
        }
        return resultado;
    }
}
