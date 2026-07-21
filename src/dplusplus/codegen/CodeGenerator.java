package dplusplus.codegen;

import dplusplus.node.*;

public class CodeGenerator {
    private String outputDir;

    public CodeGenerator(String outputDir) {
        this.outputDir = outputDir;
    }

    public void generate(Start astRoot) {
        System.out.println("[CODEGEN] Iniciando geração de código para Java...");
        generateRuntime();
        
        CodeGeneratorVisitor visitor = new CodeGeneratorVisitor(outputDir);
        astRoot.apply(visitor);
        System.out.println("[CODEGEN] Geração concluída. Arquivos salvos em: " + outputDir);
    }

    private void generateRuntime() {
        java.io.File dir = new java.io.File(outputDir);
        if (!dir.exists()) dir.mkdirs();
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(new java.io.File(dir, "Periphericals.java")))) {
            out.println("import java.util.Scanner;");
            out.println("public class Periphericals {");
            out.println("    private Scanner scanner = new Scanner(System.in);");
            out.println("    public void show(Object exp) {");
            out.println("        System.out.println(exp);");
            out.println("    }");
            out.println("    public double capture() {");
            out.println("        return scanner.nextDouble();");
            out.println("    }");
            out.println("}");
        } catch (java.io.IOException e) {
            System.err.println("[CODEGEN ERRO] Falha ao gerar classe nativa Periphericals.java: " + e.getMessage());
        }
    }
}
