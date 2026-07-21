import java.util.Scanner;

public class Programa {
    public static void main(String[] args) {
        Programa app = new Programa();
        app.main();
    }
    
    public void main() {
        Lista l = new Lista();
        Periphericals io = new Periphericals();
        double a = 5;
        double b = 10;
        double resultado = 0;
        l.inserir(1);
        l.inserir(2);
        l.inserir(3);
        resultado = a + b * 2 - b / 5;
        System.out.println(resultado);
        if (resultado > 0) {
            System.out.println(resultado);
        }
        if (a > b || resultado == 0) {
            System.out.println(a);
        }
        else {
            System.out.println(-resultado);
        }
        while (a < b) {
            a = a + 1;
            System.out.println(a);
        }
        resultado = a < b ? a + b : a - b;
        System.out.println(resultado);
    }
}
