import re

def merge():
    with open('src/dplusplus/SemanticAnalyzer.java', 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Substituir erro() por reportError() para manter o padrão
    content = content.replace('erro(', 'reportError(')
    
    # Remover o bloco HEAD que tem conflito
    # O conflito 1 começa com <<<<<<< HEAD e termina com =======
    # Seguido pelo código da tarefa-3 que termina com >>>>>>> tarefa-3-metodos-parametros-heranca
    
    # Expressão regular para remover a parte do HEAD e as marcações de conflito
    content = re.sub(r'<<<<<<< HEAD.*?=======\n', '', content, flags=re.DOTALL)
    content = re.sub(r'>>>>>>> tarefa-3-metodos-parametros-heranca\n', '', content)
    
    # E vamos apagar a implementação antiga de erro() que agora é redundante, já que temos reportError
    content = re.sub(r'private void erro\(String msg, int linha, int coluna\) \{.*?\}', '', content, flags=re.DOTALL)
    
    with open('src/dplusplus/SemanticAnalyzer.java', 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == '__main__':
    merge()
