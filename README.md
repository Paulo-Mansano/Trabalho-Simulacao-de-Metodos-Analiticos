# Trabalho-Simulacao-de-Metodos-Analiticos

# Simulador de Fila Simples - Módulo Inicial

Este é o repositório para o desenvolvimento do simulador de filas da disciplina. Conforme a especificação inicial, o objetivo é construir o sistema de forma progressiva, validando primeiro a base matemática antes de implementar a lógica de eventos da fila.

## Estado Atual do Desenvolvimento (Etapa 1)

Nesta primeira entrega, implementamos a **estrutura de configuração** e o **Gerador de Números Pseudoaleatórios (PRNG)** utilizando o *Método Congruente Linear (LCG)*.

O código foi feito em **Java**, focado em simplicidade, utilizando variáveis estáticas na classe principal para simular o escopo global exigido pela especificação.

### O que já está pronto:
1. **Parâmetros Parametrizados:** As variáveis de configuração (capacidade, servidores, intervalos de chegada/atendimento) estão centralizadas no topo do código. Para mudar de uma fila `G/G/1/5` para `G/G/2/5`, basta alterar o valor da variável `servidores` de 1 para 2.
2. **Método Congruente Linear:** A função `nextRandom()` já está operante. Ela gera números entre 0 e 1 e possui um mecanismo de segurança integrado: ao bater a marca de 100.000 chamadas (limite da simulação), ela sinaliza a parada.

### Como testar
Compile e execute o arquivo `SimuladorFila.java`. Ao rodar, ele acionará o método `main`, que neste momento serve como um script de teste e imprimirá no console os 5 primeiros números pseudoaleatórios gerados para fins de validação (podem ser cruzados com testes no Excel, se necessário).

Pra quem não lembra, é só dar javac .\SimuladorFila.java e depois java SimuladorFila

### Próximos Passos (Etapas Seguintes):
* Montar as estruturas de dados para o Escalonador de Eventos (controlar quem chega e quem sai).
* Implementar o laço principal da simulação no `main()` e as rotinas de `Chegada` e `Saída`.
