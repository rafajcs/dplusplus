package dplusplus.codegen;

import dplusplus.analysis.DepthFirstAdapter;
import dplusplus.node.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CodeGeneratorVisitor extends DepthFirstAdapter {
    private String outputDir;
    private StringBuilder currentClassCode;
    private String currentClassName;
    private int indentLevel = 0;
    private int methodDepth = 0;
    private java.util.Map<String, String> heranca = new java.util.HashMap<>();

    public CodeGeneratorVisitor(String outputDir) {
        this.outputDir = outputDir;
    }

    private void write(String text) {
        currentClassCode.append("    ".repeat(indentLevel)).append(text);
    }

    private void writeLine(String text) {
        write(text + "\n");
    }

    @Override
    public void inAPrograma(APrograma node) {
        if (node.getGenealogia() != null) {
            AGenealogia g = (AGenealogia) node.getGenealogia();
            for (PRelacao pr : g.getRelacao()) {
                ARelacao r = (ARelacao) pr;
                heranca.put(r.getFilho().getText().trim(), r.getPai().getText().trim());
            }
        }
    }

    @Override
    public void caseADeclaracaoClasse(ADeclaracaoClasse node) {
        currentClassName = node.getIdClasse().getText().trim();
        currentClassCode = new StringBuilder();
        
        // Imports padrões que podem ser necessários
        writeLine("import java.util.Scanner;");
        writeLine("");
        
        String pai = heranca.get(currentClassName);
        if (pai != null && !pai.equals("Root")) {
            writeLine("public class " + currentClassName + " extends " + pai + " {");
        } else {
            writeLine("public class " + currentClassName + " {");
        }
        
        indentLevel++;
        
        for (PComponentes comp : node.getComponentes()) {
            comp.apply(this);
        }
        
        indentLevel--;
        writeLine("}");
        
        // Salva a classe em seu próprio arquivo
        saveCurrentClass();
    }

    private void saveCurrentClass() {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, currentClassName + ".java");
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.print(currentClassCode.toString());
            System.out.println("[CODEGEN] Arquivo gerado com sucesso: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[CODEGEN ERRO] Falha ao salvar " + currentClassName + ".java: " + e.getMessage());
        }
    }

    @Override
    public void caseADecVar(ADecVar node) {
        String tipo = mapType(node.getTipoPrimitivo());
        String id = node.getId().getText().trim();
        
        ExpressionTranslator exprVisitor = new ExpressionTranslator();
        node.getExpressao().apply(exprVisitor);
        
        writeLine(tipo + " " + id + " = " + exprVisitor.getCode() + ";");
    }
    
    @Override
    public void caseADecCons(ADecCons node) {
        String tipo = mapType(node.getTipoPrimitivo());
        String id = node.getId().getText().trim();
        
        ExpressionTranslator exprVisitor = new ExpressionTranslator();
        node.getExpressao().apply(exprVisitor);
        
        writeLine("final " + tipo + " " + id + " = " + exprVisitor.getCode() + ";");
    }

    @Override
    public void caseADecObj(ADecObj node) {
        String tipo = node.getIdClasse().getText().trim();
        String id = node.getId().getText().trim();

        // Se methodDepth > 0, estamos dentro de um método (variável local) -> instanciamos para simular alocação na stack
        // Se methodDepth == 0, estamos no corpo da classe (atributo) -> iniciamos com null para evitar recursão infinita (StackOverflow)
        if (methodDepth > 0) {
            writeLine(tipo + " " + id + " = new " + tipo + "();");
        } else {
            writeLine(tipo + " " + id + " = null;");
        }
    }


    @Override
    public void caseAFuncaoConcretaMetodo(AFuncaoConcretaMetodo node) {
        String tipo = mapType(node.getTipo());
        String id = node.getId().getText().trim();
        
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < node.getParametro().size(); i++) {
            if (i > 0) params.append(", ");
            AParametro p = (AParametro) node.getParametro().get(i);
            params.append(mapType(p.getTipo())).append(" ").append(p.getId().getText().trim());
        }
        
        writeLine("public " + tipo + " " + id + "(" + params.toString() + ") {");
        indentLevel++;
        methodDepth++;
        
        node.getBlocoExp().apply(this);
        
        methodDepth--;
        indentLevel--;
        writeLine("}");
    }

    @Override
    public void caseAProcedimentoConcrMetodo(AProcedimentoConcrMetodo node) {
        String id = node.getId().getText().trim();
        boolean isMain = node.getMarcador() != null; // Marcador >> procedure main
        
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < node.getParametro().size(); i++) {
            if (i > 0) params.append(", ");
            AParametro p = (AParametro) node.getParametro().get(i);
            params.append(mapType(p.getTipo())).append(" ").append(p.getId().getText().trim());
        }
        
        if (isMain) {
            writeLine("public static void main(String[] args) {");
            indentLevel++;
            writeLine(currentClassName + " app = new " + currentClassName + "();");
            writeLine("app." + id + "();");
            indentLevel--;
            writeLine("}");
            writeLine("");
        }
        
        writeLine("public void " + id + "(" + params.toString() + ") {");
        indentLevel++;
        methodDepth++;
        
        node.getBlocoComandos().apply(this);
        
        methodDepth--;
        indentLevel--;
        writeLine("}");
    }

    @Override
    public void caseABlocoExp(ABlocoExp node) {
        for (PDecLocal dec : node.getDecLocal()) {
            dec.apply(this);
        }
        for (PComando cmd : node.getComando()) {
            cmd.apply(this);
        }
        
        ExpressionTranslator exprVisitor = new ExpressionTranslator();
        node.getExpressao().apply(exprVisitor);
        writeLine("return " + exprVisitor.getCode() + ";");
    }

    @Override
    public void caseABlocoComandos(ABlocoComandos node) {
        for (PDecLocal dec : node.getDecLocal()) {
            dec.apply(this);
        }
        for (PComando cmd : node.getComando()) {
            cmd.apply(this);
        }
    }

    @Override
    public void caseAAtribuicaoComando(AAtribuicaoComando node) {
        String id = node.getId().getText().trim();
        
        ExpressionTranslator exprVisitor = new ExpressionTranslator();
        node.getExpressao().apply(exprVisitor);
        
        writeLine(id + " = " + exprVisitor.getCode() + ";");
    }

    @Override
    public void caseAChamadaCmdComando(AChamadaCmdComando node) {
        StringBuilder prefixPath = new StringBuilder();
        for (TId prefix : node.getPrefixos()) {
            prefixPath.append(prefix.getText().trim()).append(".");
        }
        
        String method = node.getMetodo().getText().trim();
        
        if (prefixPath.toString().equals("io.") && method.equals("show")) {
            ExpressionTranslator exprVisitor = new ExpressionTranslator();
            if (!node.getExpressao().isEmpty()) {
                node.getExpressao().get(0).apply(exprVisitor);
                writeLine("System.out.println(" + exprVisitor.getCode() + ");");
            } else {
                writeLine("System.out.println();");
            }
        } else {
            StringBuilder args = new StringBuilder();
            for (int i = 0; i < node.getExpressao().size(); i++) {
                if (i > 0) args.append(", ");
                ExpressionTranslator exprVisitor = new ExpressionTranslator();
                node.getExpressao().get(i).apply(exprVisitor);
                args.append(exprVisitor.getCode());
            }
            writeLine(prefixPath.toString() + method + "(" + args.toString() + ");");
        }
    }

    @Override
    public void caseACondicionalComando(ACondicionalComando node) {
        ExpressionTranslator exprVisitor = new ExpressionTranslator();
        node.getExpressao().apply(exprVisitor);
        
        writeLine("if (" + exprVisitor.getCode() + ") {");
        indentLevel++;
        node.getBlocoComandos().apply(this);
        indentLevel--;
        writeLine("}");
        
        if (node.getOptComando() != null) {
            writeLine("else {");
            indentLevel++;
            node.getOptComando().apply(this);
            indentLevel--;
            writeLine("}");
        }
    }

    @Override
    public void caseALacoComando(ALacoComando node) {
        ExpressionTranslator exprVisitor = new ExpressionTranslator();
        node.getExpressao().apply(exprVisitor);
        
        writeLine("while (" + exprVisitor.getCode() + ") {");
        indentLevel++;
        node.getBlocoComandos().apply(this);
        indentLevel--;
        writeLine("}");
    }
    
    // Mapeamento léxico de tipos primitivos D++ para Java
    private String mapType(PTipo node) {
        if (node instanceof APrimitivoTipo) {
            return mapType(((APrimitivoTipo) node).getTipoPrimitivo());
        } else if (node instanceof AClasseTipo) {
            return ((AClasseTipo) node).getIdClasse().getText().trim();
        }
        return "Object";
    }
    
    private String mapType(PTipoPrimitivo node) {
        if (node instanceof AInteiroTipoPrimitivo) {
            // Em D++ o tipo é 'number', mapeamos para double por segurança
            return "double";
        } else if (node instanceof ABooleanoTipoPrimitivo) {
            // 'answer' mapeia para boolean
            return "boolean";
        }
        return "Object";
    }
}
