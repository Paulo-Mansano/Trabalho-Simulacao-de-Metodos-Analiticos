# Roteiro para o video

## Abertura

Neste trabalho simulamos uma rede de filas de uma assistencia tecnica de computadores e celulares. A ideia foi representar o caminho do cliente desde a recepcao ate o teste final, considerando retornos por retrabalho ou necessidade de novo diagnostico.

## Modelo inicial

O modelo tem quatro filas: Q1 para recepcao e triagem, Q2 para diagnostico, Q3 para manutencao e Q4 para teste final. A chegada externa ocorre em Q1, com intervalo uniforme entre 3 e 7 minutos. A rede nao e em tandem porque existem retornos: Q2 pode retornar para Q1, Q3 pode retornar para Q2 e Q4 pode voltar para Q3.

## Resultados iniciais

No modelo inicial, Q3 aparece como o principal gargalo pelo tempo de resposta de aproximadamente 164 minutos e pelas 1659 perdas. Q2 tambem fica praticamente saturada, com utilizacao de quase 100% e muitas perdas.

## Proposta de melhoria

A proposta altera Q3: aumentamos os servidores de 2 para 3, a capacidade de 12 para 15, reduzimos o intervalo de atendimento de U(20,40) para U(18,32) e reduzimos a probabilidade de retrabalho de Q4 para Q3 de 0,25 para 0,15.

## Comparacao

Com a melhoria, o tempo de resposta de Q3 cai de cerca de 164 para 26 minutos, uma reducao de aproximadamente 84%. As perdas em Q3 caem para zero e as perdas totais do sistema diminuem cerca de 15%. Apesar disso, Q2 continua saturada, o que indica uma oportunidade de melhoria futura.

## Fechamento

Assim, o modelo melhorado resolve o gargalo mais critico da manutencao, mas a analise mostra que a rede ainda pode evoluir com a melhoria do diagnostico tecnico.
