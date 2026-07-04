package dplusplus;

import java.io.File;

public class TestRunner {
    public static void main(String[] args) {
        // Aponta para as pastas mapeadas na diretriz de QA
        File pastaSucesso = new File("testes/fase1_exp/sucesso");
        File pastaFalha = new File("testes/fase1_exp/falha");

        System.out.println("--- Executando Casos de Sucesso (Devem dar VERDE) ---");
        runTestsInFolder(pastaSucesso, true);

        System.out.println("\n--- Executando Casos de Falha (Devem dar VERMELHO/DETECTADO) ---");
        runTestsInFolder(pastaFalha, false);
    }

    private static void runTestsInFolder(File folder, boolean expectSuccess) {
        if (!folder.exists() || !folder.isDirectory()) return;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".dpp"));
        if (files == null) return;

        for (File file : files) {
            System.out.print("Testando " + file.getName() + " -> ");
            // Aqui podes chamar o método de processamento do teu Main passando o caminho absoluto
            // Se o comportamento for o esperado (ex: pasta de falha detetou erro), printa [PASSOU]
            // Se aceitou um ficheiro com erro ou barrou um correto, printa [FALHOU EM QA]
        }
    }
}