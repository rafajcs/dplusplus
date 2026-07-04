package dplusplus;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {
    // A pilha de Tabelas Hash (cada escopo é um mapa)
    private Stack<Map<String, SymbolInfo>> scopeStack;

    public SymbolTable() {
        this.scopeStack = new Stack<>();
        
        // Inicializa com o escopo global (Base da pilha)
        enterScope();
        
        // 2. INJEÇÃO DE CLASSES NATIVAS DA LINGUAGEM
        // Registra a classe do sistema 'Periphericals' para que ela seja visível em qualquer lugar
        this.declare("Periphericals", new SymbolInfo(
            "Periphericals", 
            SymbolInfo.Category.CLASS, 
            SymbolInfo.TypeKind.CLASSE
        ));
        
        // Se a linguagem tiver outras classes nativas (ex: String, Math), injete-as aqui.
        
        
    }

    // Abre um novo escopo (chamado ao entrar em classes, métodos e start...finish)
    public void enterScope() {
        scopeStack.push(new HashMap<>());
    }

    // Fecha o escopo atual (chamado ao encontrar o finish de um bloco)
    public void exitScope() {
        if (scopeStack.size() > 1) {
            scopeStack.pop();
        } else {
            System.err.println("Erro Interno: Tentativa de desempilhar o escopo global.");
        }
    }

    // Declara um símbolo no escopo atual (no topo da pilha)
    public boolean declare(String id, SymbolInfo info) {
        Map<String, SymbolInfo> currentScope = scopeStack.peek();
        if (currentScope.containsKey(id)) {
            return false; // Erro: Identificador já declarado neste escopo
        }
        currentScope.put(id, info);
        return true;
    }

    // Busca um símbolo do topo para a base da pilha (Escopo Léxico)
    public SymbolInfo lookup(String id) {
        // Percorre a pilha do topo (tamanho-1) até a base (0)
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, SymbolInfo> scope = scopeStack.get(i);
            if (scope.containsKey(id)) {
                return scope.get(id); // Retorna o símbolo mais próximo/interno
            }
        }
        return null; // Erro: Símbolo não encontrado em nenhum escopo
    }
    
    // Verifica se existe uma declaração ativa apenas no escopo corrente
    public boolean existsInCurrentScope(String id) {
        return scopeStack.peek().containsKey(id);
    }
}