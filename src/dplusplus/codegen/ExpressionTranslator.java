package dplusplus.codegen;

import dplusplus.analysis.DepthFirstAdapter;
import dplusplus.node.*;

public class ExpressionTranslator extends DepthFirstAdapter {
    private StringBuilder code = new StringBuilder();

    public String getCode() {
        return code.toString();
    }

    @Override
    public void caseAInteiroExpressao(AInteiroExpressao node) {
        code.append(node.getValorInteiro().getText().trim());
    }

    @Override
    public void caseARealExpressao(ARealExpressao node) {
        code.append(node.getValorReal().getText().trim());
    }

    @Override
    public void caseAVerdadeiroExpressao(AVerdadeiroExpressao node) {
        code.append("true");
    }

    @Override
    public void caseAFalsoExpressao(AFalsoExpressao node) {
        code.append("false");
    }

    @Override
    public void caseAIdExpressao(AIdExpressao node) {
        code.append(node.getId().getText().trim());
    }

    @Override
    public void caseASomaExpressao(ASomaExpressao node) {
        node.getEsq().apply(this);
        code.append(" + ");
        node.getDir().apply(this);
    }

    @Override
    public void caseASubtracaoExpressao(ASubtracaoExpressao node) {
        node.getEsq().apply(this);
        code.append(" - ");
        node.getDir().apply(this);
    }

    @Override
    public void caseAMultExpressao(AMultExpressao node) {
        node.getEsq().apply(this);
        code.append(" * ");
        node.getDir().apply(this);
    }

    @Override
    public void caseADivExpressao(ADivExpressao node) {
        node.getEsq().apply(this);
        code.append(" / ");
        node.getDir().apply(this);
    }

    @Override
    public void caseAMaiorExpressao(AMaiorExpressao node) {
        node.getEsq().apply(this);
        code.append(" > ");
        node.getDir().apply(this);
    }

    @Override
    public void caseAMenorExpressao(AMenorExpressao node) {
        node.getEsq().apply(this);
        code.append(" < ");
        node.getDir().apply(this);
    }

    @Override
    public void caseAIgualExpressao(AIgualExpressao node) {
        node.getEsq().apply(this);
        code.append(" == ");
        node.getDir().apply(this);
    }
    
    @Override
    public void caseAOuExpressao(AOuExpressao node) {
        node.getEsq().apply(this);
        code.append(" || ");
        node.getDir().apply(this);
    }
    
    @Override
    public void caseAEExpressao(AEExpressao node) {
        node.getEsq().apply(this);
        code.append(" && ");
        node.getDir().apply(this);
    }
    
    @Override
    public void caseAMenosUnarioExpressao(AMenosUnarioExpressao node) {
        code.append("-");
        node.getExpressao().apply(this);
    }
    
    @Override
    public void caseANegacaoExpressao(ANegacaoExpressao node) {
        code.append("!");
        node.getExpressao().apply(this);
    }

    @Override
    public void caseAAcessoExpressao(AAcessoExpressao node) {
        StringBuilder prefixPath = new StringBuilder();
        for (TId prefix : node.getPrefixos()) {
            prefixPath.append(prefix.getText().trim()).append(".");
        }
        
        String attribute = node.getAtributo().getText().trim();
        code.append(prefixPath.toString()).append(attribute);
    }

    @Override
    public void caseAChamadaExpressao(AChamadaExpressao node) {
        StringBuilder prefixPath = new StringBuilder();
        for (TId prefix : node.getPrefixos()) {
            prefixPath.append(prefix.getText().trim()).append(".");
        }
        
        String method = node.getMetodo().getText().trim();
        
        if (prefixPath.toString().equals("io.") && method.equals("capture")) {
            code.append("new java.util.Scanner(System.in).nextDouble()");
        } else {
            code.append(prefixPath.toString()).append(method).append("(");
            
            java.util.List<PExpressao> args = node.getExpressao();
            for (int i = 0; i < args.size(); i++) {
                ExpressionTranslator argTranslator = new ExpressionTranslator();
                args.get(i).apply(argTranslator);
                code.append(argTranslator.getCode());
                if (i < args.size() - 1) {
                    code.append(", ");
                }
            }
            code.append(")");
        }
    }

    @Override
    public void caseATernarioExpressao(ATernarioExpressao node) {
        node.getCond().apply(this);
        code.append(" ? ");
        node.getVTrue().apply(this);
        code.append(" : ");
        node.getVFalse().apply(this);
    }
}
