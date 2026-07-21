import java.util.Scanner;
public class Periphericals {
    private Scanner scanner = new Scanner(System.in);
    public void show(Object exp) {
        System.out.println(exp);
    }
    public double capture() {
        return scanner.nextDouble();
    }
}
