# D++ Compiler

Compilador acadêmico desenvolvido durante a disciplina de Compiladores utilizando **Java** e **SableCC** para geração automática do analisador léxico, parser e estruturas internas da linguagem.

Repositório oficial:
[D++ Compiler Repository](https://github.com/rafajcs/dplusplus)

---

## 📚 Sobre o Projeto

O **D++** é um compilador em desenvolvimento criado com foco no estudo prático da construção de compiladores e linguagens formais.

O projeto utiliza o **SableCC** para gerar automaticamente:

* Analisador léxico
* Parser sintático
* AST (Abstract Syntax Tree)
* Estruturas de navegação da árvore sintática

A implementação e os testes são realizados em **Java**, permitindo validar programas escritos na linguagem D++ durante o desenvolvimento do compilador.

---

## ⚙️ Tecnologias Utilizadas

* **Java**
* **[SableCC](https://sablecc.org)**
* **JVM**
* **Makefile / Scripts de build**
* **Git**

---

## 🧠 Objetivos do Projeto

O compilador foi desenvolvido para estudar:

* Linguagens formais
* Construção de compiladores
* Geração automática de parsers
* Árvores sintáticas (AST)
* Análise léxica e sintática
* Estruturas internas de compiladores
* Interpretação e validação de código-fonte

---

## 📁 Estrutura do Projeto

```text id="91wt4n"
.
├── src/
│   ├── lexer/
│   ├── parser/
│   ├── analysis/
│   ├── node/
│   ├── Main.java
│   └── ...
│
├── grammar/
│   └── dplusplus.sable
│
├── tests/
│   ├── example1.dpp
│   ├── example2.dpp
│   └── ...
│
├── generated/
│
├── Makefile
└── README.md
```

---

## 🔨 Como Compilar

### Pré-requisitos

* Java JDK 21
* SableCC

---

## ▶️ Gerar Parser com SableCC

Exemplo de geração do parser:

```bash id="36j2y0"
java -jar sablecc.jar grammar/dplusplus.sable
```

O SableCC irá gerar automaticamente:

* Lexer
* Parser
* Classes da AST
* Visitors
* Estruturas de análise

---

## ☕ Compilar o Projeto Java

```bash id="1rlj1q"
javac src/**/*.java
```

Ou utilizando Makefile:

```bash id="uy4i0p"
make
```

---

## 🚀 Executar o Compilador

```bash id="m1u7gk"
java Main tests/example1.dpp
```

---

## 🧪 Exemplo de Código Fonte

```D++
family Operacoes_mat start
    object Periphericals io.

    function number calcula_fatorial [] start

        alterable number n << io.capture[].
        alterable number resultado << 1.

        -- caso o usuário digite um número negativo
        in case that (n < 0) start
            io.show[-10000000000].
        finish

        in case that (n > 1) start
            as long as (n > 1) start
                resultado << resultado*n.
                n << n - 1.
            finish    
            resultado
        finish
        otherwise resultado

    finish

finish

```

---

## 🏗️ Fluxo do Compilador

```text id="mjlwmn"
Código Fonte
     ↓
Lexer (SableCC)
     ↓
Parser (SableCC)
     ↓
AST
     ↓
Análise Semântica
     ↓
Execução/Testes em Java
```

---

## 📖 Componentes Gerados pelo SableCC

O SableCC gera automaticamente diversas estruturas importantes:

* Tokens
* Nós da AST
* Visitors
* Classes de análise
* Parser LR
* Estruturas de navegação sintática

Isso permite focar no desenvolvimento da linguagem e da lógica do compilador.

---

## 🚧 Funcionalidades Futuras

* Verificação semântica completa
* Tabela de símbolos
* Inferência de tipos
* Geração de código intermediário
* Interpretador
* Geração de assembly
* Otimizações
* Melhor tratamento de erros
* Suporte a funções e escopo

---

## 🧪 Testes

Os arquivos de teste ficam na pasta:

```
tests/
```

Exemplo de execução:

```
java Main tests/example2.dp
```

---

## 👨‍💻 Autores: Cauan Machado, Rafael Coelho, Tassio Mateus e Vinicius Leite.

Projeto acadêmico desenvolvido durante a disciplina de Compiladores com foco em estudo de parsing, análise sintática e construção de linguagens utilizando Java e SableCC.
