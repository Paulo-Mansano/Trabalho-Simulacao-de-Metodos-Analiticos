# Apresentacao T2 - Simulacao de rede de filas

## Slide 1 - Capa

**Simulacao de rede de filas em uma assistencia tecnica**

Integrantes: Andrei Boeck, Guilherme Tavares, Paulo Tavares e Thomaz Abrantes.

## Slide 2 - Realidade simulada

O sistema modela uma assistencia tecnica que recebe computadores e celulares para triagem, diagnostico, manutencao e teste final. O processo possui retrabalho: alguns clientes retornam para triagem, diagnostico ou reparo quando ha falha, duvida tecnica ou necessidade de novo ajuste.

## Slide 3 - Modelo inicial da rede

Filas:

| Fila | Etapa | Kendall | Atendimento | Servidores | Capacidade |
| --- | --- | --- | --- | ---: | ---: |
| Q1 | Recepcao e triagem | G/G/1/10 | U(2, 5) | 1 | 10 |
| Q2 | Diagnostico tecnico | G/G/1/8 | U(8, 15) | 1 | 8 |
| Q3 | Manutencao/reparo | G/G/2/12 | U(20, 40) | 2 | 12 |
| Q4 | Teste final | G/G/1/6 | U(5, 12) | 1 | 6 |

Chegada externa em Q1: primeira chegada no tempo 2.0 e intervalo U(3, 7).

Roteamento: Q1->Q2 1.00; Q2->Q3 0.80; Q2->Q1 0.10; Q2->saida 0.10; Q3->Q4 0.85; Q3->Q2 0.15; Q4->Q3 0.25; Q4->saida 0.75.

## Slide 4 - Resultados do modelo inicial

| Fila | Pop. media | Vazao | Utilizacao | Tempo resp. | Perdas |
| --- | ---: | ---: | ---: | ---: | ---: |
| Q1 | 0.831138 | 0.209583 | 73.49% | 3.965675 | 0 |
| Q2 | 7.772198 | 0.086858 | 99.99% | 89.481745 | 12703 |
| Q3 | 10.957680 | 0.066666 | 99.98% | 164.367687 | 1659 |
| Q4 | 0.544200 | 0.057100 | 48.28% | 9.530580 | 0 |

Leitura: Q3 apresenta o maior tempo de resposta e tambem gera perdas; Q2 esta praticamente saturada.

## Slide 5 - Probabilidades dos estados mais relevantes

No modelo inicial, Q2 permanece cheia em 77.44% do tempo e Q3 permanece nos estados 10, 11 e 12 em cerca de 92.15% do tempo. Isso confirma a existencia de gargalos antes da proposta de melhoria.

## Slide 6 - Proposta de melhoria

A melhoria altera Q3, que representa manutencao/reparo:

| Parametro | Inicial | Melhorado |
| --- | ---: | ---: |
| Servidores | 2 | 3 |
| Capacidade | 12 | 15 |
| Atendimento | U(20, 40) | U(18, 32) |
| Retorno Q4->Q3 | 0.25 | 0.15 |
| Saida em Q4 | 0.75 | 0.85 |

Justificativa: a equipe adiciona um tecnico de reparo, aumenta a area de espera e melhora o processo de teste para reduzir retrabalho.

## Slide 7 - Resultados do modelo melhorado

| Fila | Pop. media | Vazao | Utilizacao | Tempo resp. | Perdas |
| --- | ---: | ---: | ---: | ---: | ---: |
| Q1 | 0.814222 | 0.208458 | 73.00% | 3.905925 | 0 |
| Q2 | 7.770217 | 0.086857 | 99.99% | 89.460288 | 12218 |
| Q3 | 2.062671 | 0.079662 | 66.48% | 25.892771 | 0 |
| Q4 | 0.717103 | 0.068392 | 58.16% | 10.485194 | 0 |

## Slide 8 - Comparacao inicial versus melhorado

| Indicador | Inicial | Melhorado | Variacao |
| --- | ---: | ---: | ---: |
| Tempo de resposta Q3 | 164.367687 | 25.892771 | -84.25% |
| Perdas em Q3 | 1659 | 0 | -100.00% |
| Perdas totais | 14362 | 12218 | -14.93% |
| Taxa de perdas | 0.14948512 | 0.13278454 | -11.17% |
| Tempo de resposta Q4 | 9.530580 | 10.485194 | +10.02% |

## Slide 9 - Conclusoes finais

A melhoria proposta reduz o principal gargalo identificado em Q3 e elimina perdas nessa etapa. A rede melhorada tambem diminui as perdas totais do sistema. Entretanto, Q2 permanece praticamente saturada, entao uma proxima melhoria deveria considerar aumento de capacidade, mais servidores ou reducao do tempo de diagnostico tecnico.

## Slide 10 - Arquivos entregues

- `modelo_inicial.yml`
- `modelo_melhorado.yml`
- `SimuladorFila.java`
- `resultados_simulacao.txt`
- `apresentacao_t2.md`
- `roteiro_video.md`

O video deve comentar a apresentacao, explicar a realidade simulada, a rede de filas, os resultados, a proposta de melhoria e a comparacao final.
