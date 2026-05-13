//===================================================================
// Andrei Böeck, Guilherme Tavares, Paulo Tavares e Thomaz Abrantes
//===================================================================

//=======================================
// Como executar:
// 1. Compile: javac SimuladorFila.java
// 2. Execute: java SimuladorFila
//=======================================

import java.util.ArrayList;
import java.util.List;

public class SimuladorFila {

    // =========================
    // PARAMETROS DA SIMULACAO
    // =========================

    static int limiteAleatorios = 100000;
    static double tempoPrimeiraChegada = 2.0;

    // Chegadas externas na Recepcao
    static double chegadaMin = 3.0;
    static double chegadaMax = 7.0;

    // =========================
    // PARAMETROS DO LCG
    // =========================

    static long m = 2147483647L; // 2^31 - 1
    static long a = 1103515245L;
    static long c = 12345L;
    static long sementeInicial = 42L;

    static long sementeAtual = sementeInicial;
    static int aleatoriosConsumidos = 0;

    // =========================
    // CLASSE FILA
    // =========================

    static class Fila {
        String nome;

        int capacidade;
        int servidores;

        double atendimentoMin;
        double atendimentoMax;

        int clientesNoSistema = 0;
        int servidoresOcupados = 0;

        int perdas = 0;
        int atendidos = 0;
        int chegadasAceitas = 0;

        double[] temposEstados;
        double[] saidasServidores;

        double tempoOcupadoAcumulado = 0.0;

        Fila(String nome, int capacidade, int servidores, double atendimentoMin, double atendimentoMax) {
            this.nome = nome;
            this.capacidade = capacidade;
            this.servidores = servidores;
            this.atendimentoMin = atendimentoMin;
            this.atendimentoMax = atendimentoMax;

            this.temposEstados = new double[capacidade + 1];
            this.saidasServidores = new double[servidores];

            for (int i = 0; i < servidores; i++) {
                this.saidasServidores[i] = Double.POSITIVE_INFINITY;
            }
        }

        double tempoMedioAtendimento() {
            return (atendimentoMin + atendimentoMax) / 2.0;
        }

        double taxaMediaAtendimento() {
            return 1.0 / tempoMedioAtendimento();
        }
    }

    // =========================
    // RESULTADO
    // =========================

    static class ResultadoSimulacao {
        String nomeModelo;
        double tempoGlobal;
        int aleatoriosUsados;
        int clientesQueSairamDoSistema;
        List<Fila> filas;
    }

    // =========================
    // GERADOR ALEATORIO
    // =========================

    public static void resetGerador() {
        sementeAtual = sementeInicial;
        aleatoriosConsumidos = 0;
    }

    public static Double nextRandom() {
        if (aleatoriosConsumidos >= limiteAleatorios) {
            return null;
        }

        sementeAtual = (a * sementeAtual + c) % m;
        aleatoriosConsumidos++;

        return (double) sementeAtual / m;
    }

    public static Double calcularTempo(double min, double max) {
        Double u = nextRandom();

        if (u == null) {
            return null;
        }

        return min + (max - min) * u;
    }

    // =========================
    // FUNCOES AUXILIARES
    // =========================

    public static int indiceServidorLivre(Fila fila) {
        for (int i = 0; i < fila.saidasServidores.length; i++) {
            if (Double.isInfinite(fila.saidasServidores[i])) {
                return i;
            }
        }

        return -1;
    }

    public static int indiceProximaSaida(Fila fila) {
        int indice = -1;
        double menorTempo = Double.POSITIVE_INFINITY;

        for (int i = 0; i < fila.saidasServidores.length; i++) {
            if (fila.saidasServidores[i] < menorTempo) {
                menorTempo = fila.saidasServidores[i];
                indice = i;
            }
        }

        return indice;
    }

    public static void atualizarEstatisticasTempo(List<Fila> filas, double deltaTempo) {
        for (Fila fila : filas) {
            fila.temposEstados[fila.clientesNoSistema] += deltaTempo;
            fila.tempoOcupadoAcumulado += fila.servidoresOcupados * deltaTempo;
        }
    }

    public static boolean iniciarAtendimentoSePossivel(Fila fila, double tempoAtual) {
        if (fila.servidoresOcupados >= fila.servidores) {
            return true;
        }

        if (fila.clientesNoSistema <= fila.servidoresOcupados) {
            return true;
        }

        int indiceLivre = indiceServidorLivre(fila);

        if (indiceLivre == -1) {
            return true;
        }

        Double tempoAtendimento = calcularTempo(fila.atendimentoMin, fila.atendimentoMax);

        if (tempoAtendimento == null) {
            return false;
        }

        fila.servidoresOcupados++;
        fila.saidasServidores[indiceLivre] = tempoAtual + tempoAtendimento;

        return true;
    }

    public static boolean chegadaNaFila(Fila fila, double tempoAtual) {
        if (fila.clientesNoSistema < fila.capacidade) {
            fila.clientesNoSistema++;
            fila.chegadasAceitas++;

            return iniciarAtendimentoSePossivel(fila, tempoAtual);
        } else {
            fila.perdas++;
            return true;
        }
    }

    // =========================
    // ROTEAMENTO
    // =========================

    public static Fila escolherDestino(
            Fila origem,
            Fila recepcao,
            Fila diagnostico,
            Fila reparo,
            Fila testeFinal,
            boolean modeloMelhorado
    ) {
        Double u = nextRandom();

        if (u == null) {
            return null;
        }

        if (origem == recepcao) {
            // Recepcao -> Diagnostico: 100%
            return diagnostico;
        }

        if (origem == diagnostico) {
            // Diagnostico -> Reparo: 80%
            // Diagnostico -> Recepcao: 10%
            // Diagnostico -> Saida: 10%

            if (u < 0.80) {
                return reparo;
            } else if (u < 0.90) {
                return recepcao;
            } else {
                return null; // Sai do sistema
            }
        }

        if (origem == reparo) {
            // Reparo -> Teste Final: 85%
            // Reparo -> Diagnostico: 15%

            if (u < 0.85) {
                return testeFinal;
            } else {
                return diagnostico;
            }
        }

        if (origem == testeFinal) {
            if (modeloMelhorado) {
                // Modelo melhorado:
                // Teste Final -> Saida: 85%
                // Teste Final -> Reparo: 15%

                if (u < 0.85) {
                    return null; // Sai do sistema
                } else {
                    return reparo;
                }
            } else {
                // Modelo inicial:
                // Teste Final -> Saida: 75%
                // Teste Final -> Reparo: 25%

                if (u < 0.75) {
                    return null; // Sai do sistema
                } else {
                    return reparo;
                }
            }
        }

        return null;
    }

    // =========================
    // SIMULACAO
    // =========================

    public static ResultadoSimulacao simular(String nomeModelo, boolean modeloMelhorado) {
        resetGerador();

        double tempoAtual = 0.0;
        double proximaChegadaExterna = tempoPrimeiraChegada;

        int clientesQueSairamDoSistema = 0;

        Fila recepcao = new Fila(
                "Recepcao/Triagem",
                10,
                1,
                2.0,
                5.0
        );

        Fila diagnostico = new Fila(
                "Diagnostico Tecnico",
                8,
                1,
                8.0,
                15.0
        );

        Fila reparo;

        if (modeloMelhorado) {
            reparo = new Fila(
                    "Reparo",
                    15,
                    3,
                    18.0,
                    32.0
            );
        } else {
            reparo = new Fila(
                    "Reparo",
                    12,
                    2,
                    20.0,
                    40.0
            );
        }

        Fila testeFinal = new Fila(
                "Teste Final",
                6,
                1,
                5.0,
                12.0
        );

        List<Fila> filas = new ArrayList<>();
        filas.add(recepcao);
        filas.add(diagnostico);
        filas.add(reparo);
        filas.add(testeFinal);

        boolean encerrar = false;

        while (!encerrar) {
            Fila filaProximaSaida = null;
            int indiceServidorSaida = -1;
            double tempoProximaSaida = Double.POSITIVE_INFINITY;

            for (Fila fila : filas) {
                int indice = indiceProximaSaida(fila);

                if (indice != -1) {
                    double tempoSaida = fila.saidasServidores[indice];

                    if (tempoSaida < tempoProximaSaida) {
                        tempoProximaSaida = tempoSaida;
                        filaProximaSaida = fila;
                        indiceServidorSaida = indice;
                    }
                }
            }

            double proximoEvento = Math.min(proximaChegadaExterna, tempoProximaSaida);

            if (Double.isInfinite(proximoEvento)) {
                break;
            }

            double deltaTempo = proximoEvento - tempoAtual;
            atualizarEstatisticasTempo(filas, deltaTempo);
            tempoAtual = proximoEvento;

            // Em empate, processa saida antes de chegada externa
            if (tempoProximaSaida <= proximaChegadaExterna) {
                Fila fila = filaProximaSaida;

                fila.saidasServidores[indiceServidorSaida] = Double.POSITIVE_INFINITY;
                fila.clientesNoSistema--;
                fila.servidoresOcupados--;
                fila.atendidos++;

                // Se ainda tem cliente esperando nessa mesma fila, inicia novo atendimento
                boolean ok = iniciarAtendimentoSePossivel(fila, tempoAtual);

                if (!ok) {
                    encerrar = true;
                    continue;
                }

                Fila destino = escolherDestino(
                        fila,
                        recepcao,
                        diagnostico,
                        reparo,
                        testeFinal,
                        modeloMelhorado
                );

                // Se destino for null, o cliente saiu do sistema
                if (destino == null) {
                    clientesQueSairamDoSistema++;
                } else {
                    ok = chegadaNaFila(destino, tempoAtual);

                    if (!ok) {
                        encerrar = true;
                    }
                }

            } else {
                // Chegada externa sempre entra pela Recepcao
                boolean ok = chegadaNaFila(recepcao, tempoAtual);

                if (!ok) {
                    encerrar = true;
                    continue;
                }

                Double tempoEntreChegadas = calcularTempo(chegadaMin, chegadaMax);

                if (tempoEntreChegadas == null) {
                    proximaChegadaExterna = Double.POSITIVE_INFINITY;
                    encerrar = true;
                } else {
                    proximaChegadaExterna = tempoAtual + tempoEntreChegadas;
                }
            }

            if (aleatoriosConsumidos >= limiteAleatorios) {
                encerrar = true;
            }
        }

        ResultadoSimulacao resultado = new ResultadoSimulacao();
        resultado.nomeModelo = nomeModelo;
        resultado.tempoGlobal = tempoAtual;
        resultado.aleatoriosUsados = aleatoriosConsumidos;
        resultado.clientesQueSairamDoSistema = clientesQueSairamDoSistema;
        resultado.filas = filas;

        return resultado;
    }

    // =========================
    // CALCULOS DOS INDICES
    // =========================

    public static double calcularPopulacaoMedia(Fila fila, double tempoGlobal) {
        double populacaoMedia = 0.0;

        for (int i = 0; i < fila.temposEstados.length; i++) {
            double probabilidade = fila.temposEstados[i] / tempoGlobal;
            populacaoMedia += i * probabilidade;
        }

        return populacaoMedia;
    }

    public static double calcularVazao(Fila fila, double tempoGlobal) {
        return fila.atendidos / tempoGlobal;
    }

    public static double calcularUtilizacao(Fila fila, double tempoGlobal) {
        return fila.tempoOcupadoAcumulado / (tempoGlobal * fila.servidores);
    }

    public static double calcularTempoResposta(double populacaoMedia, double vazao) {
        if (vazao == 0.0) {
            return 0.0;
        }

        return populacaoMedia / vazao;
    }

    public static int calcularPerdasTotais(ResultadoSimulacao r) {
        int perdasTotais = 0;

        for (Fila fila : r.filas) {
            perdasTotais += fila.perdas;
        }

        return perdasTotais;
    }

    public static double calcularTaxaPerdas(ResultadoSimulacao r) {
        if (r.tempoGlobal == 0.0) {
            return 0.0;
        }

        return calcularPerdasTotais(r) / r.tempoGlobal;
    }

    public static void imprimirResultado(ResultadoSimulacao r) {
        System.out.println();
        System.out.println("==========================================================");
        System.out.println("RESULTADO DA SIMULACAO: " + r.nomeModelo);
        System.out.println("==========================================================");

        System.out.printf("Tempo global da simulacao: %.6f minutos%n", r.tempoGlobal);
        System.out.printf("Aleatorios usados: %d%n", r.aleatoriosUsados);
        System.out.printf("Clientes que sairam do sistema: %d%n", r.clientesQueSairamDoSistema);

        System.out.println();
        System.out.println("PARAMETROS DAS FILAS:");
        System.out.println("----------------------------------------------------------");

        for (Fila fila : r.filas) {
            System.out.printf(
                    "%s | G/G/%d/%d | Atendimento: %.2f a %.2f min | MU medio: %.6f clientes/min%n",
                    fila.nome,
                    fila.servidores,
                    fila.capacidade,
                    fila.atendimentoMin,
                    fila.atendimentoMax,
                    fila.taxaMediaAtendimento()
            );
        }

        System.out.println();
        System.out.println("INDICES DE DESEMPENHO:");
        System.out.println("----------------------------------------------------------");

        System.out.printf(
                "%-22s %12s %12s %12s %16s %10s%n",
                "Fila",
                "Pop.Media",
                "Vazao",
                "Utilizacao",
                "Tempo Resp.",
                "Perdas"
        );

        for (Fila fila : r.filas) {
            double populacaoMedia = calcularPopulacaoMedia(fila, r.tempoGlobal);
            double vazao = calcularVazao(fila, r.tempoGlobal);
            double utilizacao = calcularUtilizacao(fila, r.tempoGlobal);
            double tempoResposta = calcularTempoResposta(populacaoMedia, vazao);

            System.out.printf(
                    "%-22s %12.6f %12.6f %11.2f%% %16.6f %10d%n",
                    fila.nome,
                    populacaoMedia,
                    vazao,
                    utilizacao * 100.0,
                    tempoResposta,
                    fila.perdas
            );
        }

        System.out.println();
        System.out.println("PROBABILIDADES DOS ESTADOS:");
        System.out.println("----------------------------------------------------------");

        for (Fila fila : r.filas) {
            System.out.println();
            System.out.println("Fila: " + fila.nome);

            for (int estado = 0; estado < fila.temposEstados.length; estado++) {
                double tempoEstado = fila.temposEstados[estado];
                double probabilidade = tempoEstado / r.tempoGlobal;

                System.out.printf(
                        "Estado %2d: tempo = %12.6f | probabilidade = %.8f%n",
                        estado,
                        tempoEstado,
                        probabilidade
                );
            }
        }
    }

    public static void imprimirComparacao(ResultadoSimulacao inicial, ResultadoSimulacao melhorado) {
        System.out.println();
        System.out.println("==========================================================");
        System.out.println("COMPARACAO ENTRE MODELO INICIAL E MODELO MELHORADO");
        System.out.println("==========================================================");

        System.out.printf(
                "%-22s %16s %16s %20s%n",
                "Fila",
                "R Inicial",
                "R Melhorado",
                "Variacao Absoluta"
        );

        for (int i = 0; i < inicial.filas.size(); i++) {
            Fila filaInicial = inicial.filas.get(i);
            Fila filaMelhorada = melhorado.filas.get(i);

            double nInicial = calcularPopulacaoMedia(filaInicial, inicial.tempoGlobal);
            double xInicial = calcularVazao(filaInicial, inicial.tempoGlobal);
            double rInicial = calcularTempoResposta(nInicial, xInicial);

            double nMelhorado = calcularPopulacaoMedia(filaMelhorada, melhorado.tempoGlobal);
            double xMelhorado = calcularVazao(filaMelhorada, melhorado.tempoGlobal);
            double rMelhorado = calcularTempoResposta(nMelhorado, xMelhorado);

            double variacaoAbsoluta = 0.0;

            if (rInicial != 0.0) {
                variacaoAbsoluta = Math.abs(((rMelhorado - rInicial) / rInicial) * 100.0);
            }

            System.out.printf(
                    "%-22s %16.6f %16.6f %19.2f%%%n",
                    filaInicial.nome,
                    rInicial,
                    rMelhorado,
                    variacaoAbsoluta
            );
        }

        int perdasInicial = calcularPerdasTotais(inicial);
        int perdasMelhorado = calcularPerdasTotais(melhorado);

        double variacaoPerdasAbsoluta = 0.0;

        if (perdasInicial != 0) {
            variacaoPerdasAbsoluta = Math.abs(((double) (perdasMelhorado - perdasInicial) / perdasInicial) * 100.0);
        }

        double taxaPerdasInicial = calcularTaxaPerdas(inicial);
        double taxaPerdasMelhorado = calcularTaxaPerdas(melhorado);

        double variacaoTaxaPerdasAbsoluta = 0.0;

        if (taxaPerdasInicial != 0.0) {
            variacaoTaxaPerdasAbsoluta = Math.abs(
                    ((taxaPerdasMelhorado - taxaPerdasInicial) / taxaPerdasInicial) * 100.0
            );
        }

        System.out.println();
        System.out.printf("Perdas totais no modelo inicial: %d%n", perdasInicial);
        System.out.printf("Perdas totais no modelo melhorado: %d%n", perdasMelhorado);
        System.out.printf("Variacao absoluta das perdas totais: %.2f%%%n", variacaoPerdasAbsoluta);

        System.out.println();
        System.out.printf("Taxa de perdas inicial: %.8f perdas/min%n", taxaPerdasInicial);
        System.out.printf("Taxa de perdas melhorada: %.8f perdas/min%n", taxaPerdasMelhorado);
        System.out.printf("Variacao absoluta da taxa de perdas: %.2f%%%n", variacaoTaxaPerdasAbsoluta);
    }

    // =========================
    // MAIN
    // =========================

    public static void main(String[] args) {
        ResultadoSimulacao modeloInicial = simular("Modelo Inicial", false);
        ResultadoSimulacao modeloMelhorado = simular("Modelo Melhorado", true);

        imprimirResultado(modeloInicial);
        imprimirResultado(modeloMelhorado);
        imprimirComparacao(modeloInicial, modeloMelhorado);
    }
}