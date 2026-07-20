package dplusplus.codegen;

import dplusplus.node.*;

public class CodeGenerator {
    private String outputDir;

    public CodeGenerator(String outputDir) {
        this.outputDir = outputDir;
    }

    public void generate(Start astRoot) {
        System.out.println("[CODEGEN] Iniciando geração de código para Java...");
        CodeGeneratorVisitor visitor = new CodeGeneratorVisitor(outputDir);
        astRoot.apply(visitor);
        System.out.println("[CODEGEN] Geração concluída. Arquivos salvos em: " + outputDir);
    }
}
