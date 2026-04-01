public class SimuladorFila {

    // Parametros da simulacao
    static int capacidadeFila = 5;
    static double chegadaMin = 2.0;
    static double chegadaMax = 5.0;
    static double atendimentoMin = 3.0;
    static double atendimentoMax = 5.0;
    static int limiteAleatorios = 100000;
    static double tempoPrimeiraChegada = 2.0;

    // Parametros do LCG
    static long m = 2147483647L; // 2^31 - 1
    static long a = 1103515245L;
    static long c = 12345L;
    static long sementeInicial = 42L;

    static long sementeAtual = sementeInicial;
    static int aleatoriosConsumidos = 0;
    static boolean limiteAtingidoNesteEvento = false;

    static class ResultadoSimulacao {
        int servidores;
        int capacidade;
        int perdas;
        int aleatoriosUsados;
        double tempoGlobal;
        double[] temposEstados;
    }

    public static void resetGerador() {
        sementeAtual = sementeInicial;
        aleatoriosConsumidos = 0;
        limiteAtingidoNesteEvento = false;
    }

    public static Double nextRandom() {
        if (aleatoriosConsumidos >= limiteAleatorios) {
            return null;
        }

        sementeAtual = (a * sementeAtual + c) % m;
        aleatoriosConsumidos++;

        if (aleatoriosConsumidos == limiteAleatorios) {
            limiteAtingidoNesteEvento = true;
        }

        return (double) sementeAtual / m;
    }

    public static Double calcularTempo(double min, double max) {
        Double u = nextRandom();
        if (u == null) {
            return null;
        }
        return min + (max - min) * u;
    }

    // Retorna o indice do servidor com a menor saida agendada.
    // Se nenhum servidor estiver ocupado, retorna -1.
    public static int indiceProximaSaida(double[] saidasServidores) {
        int indice = -1;
        double menorTempo = Double.POSITIVE_INFINITY;

        for (int i = 0; i < saidasServidores.length; i++) {
            if (saidasServidores[i] < menorTempo) {
                menorTempo = saidasServidores[i];
                indice = i;
            }
        }

        return indice;
    }

    // Retorna o indice de um servidor livre (sem saida agendada).
    // Se todos estiverem ocupados, retorna -1.
    public static int indiceServidorLivre(double[] saidasServidores) {
        for (int i = 0; i < saidasServidores.length; i++) {
            if (Double.isInfinite(saidasServidores[i])) {
                return i;
            }
        }
        return -1;
    }

    public static ResultadoSimulacao simular(int servidores) {
        resetGerador();

        double tempoAtual = 0.0;
        int clientesNoSistema = 0;
        int servidoresOcupados = 0;
        int perdas = 0;

        double[] temposEstados = new double[capacidadeFila + 1];

        double proximaChegada = tempoPrimeiraChegada;
        double[] saidasServidores = new double[servidores];
        for (int i = 0; i < servidores; i++) {
            saidasServidores[i] = Double.POSITIVE_INFINITY;
        }

        boolean encerrar = false;

        while (!encerrar) {
            int indiceSaida = indiceProximaSaida(saidasServidores);
            double proximaSaida = (indiceSaida == -1) ? Double.POSITIVE_INFINITY : saidasServidores[indiceSaida];
            double proximoEvento = Math.min(proximaChegada, proximaSaida);

            if (Double.isInfinite(proximoEvento)) {
                break;
            }

            temposEstados[clientesNoSistema] += (proximoEvento - tempoAtual);
            tempoAtual = proximoEvento;
            limiteAtingidoNesteEvento = false;

            // Em empate, processa saida antes de chegada.
            if (proximaSaida <= proximaChegada) {
                saidasServidores[indiceSaida] = Double.POSITIVE_INFINITY;
                clientesNoSistema--;
                servidoresOcupados--;

                // Se existe fila de espera, inicia novo atendimento imediatamente.
                if (clientesNoSistema > servidoresOcupados) {
                    servidoresOcupados++;
                    Double tempoAtendimento = calcularTempo(atendimentoMin, atendimentoMax);
                    if (tempoAtendimento == null) {
                        encerrar = true;
                    } else {
                        saidasServidores[indiceSaida] = tempoAtual + tempoAtendimento;
                    }
                }
            } else {
                if (clientesNoSistema < capacidadeFila) {
                    clientesNoSistema++;

                    if (servidoresOcupados < servidores) {
                        int indiceLivre = indiceServidorLivre(saidasServidores);
                        servidoresOcupados++;
                        Double tempoAtendimento = calcularTempo(atendimentoMin, atendimentoMax);
                        if (tempoAtendimento == null) {
                            encerrar = true;
                        } else {
                            if (indiceLivre != -1) {
                                saidasServidores[indiceLivre] = tempoAtual + tempoAtendimento;
                            }
                        }
                    }
                } else {
                    perdas++;
                }

                Double tempoEntreChegadas = calcularTempo(chegadaMin, chegadaMax);
                if (tempoEntreChegadas == null) {
                    proximaChegada = Double.POSITIVE_INFINITY;
                    encerrar = true;
                } else {
                    proximaChegada = tempoAtual + tempoEntreChegadas;
                }
            }

            if (limiteAtingidoNesteEvento) {
                encerrar = true;
            }
        }

        ResultadoSimulacao resultado = new ResultadoSimulacao();
        resultado.servidores = servidores;
        resultado.capacidade = capacidadeFila;
        resultado.perdas = perdas;
        resultado.aleatoriosUsados = aleatoriosConsumidos;
        resultado.tempoGlobal = tempoAtual;
        resultado.temposEstados = temposEstados;
        return resultado;
    }

    public static void imprimirResultado(ResultadoSimulacao r) {
        System.out.printf("%n=== Resultado G/G/%d/%d ===%n", r.servidores, r.capacidade);
        System.out.printf("Aleatorios usados: %d%n", r.aleatoriosUsados);
        System.out.printf("Tempo global da simulacao: %.6f%n", r.tempoGlobal);
        System.out.printf("Perdas de clientes: %d%n", r.perdas);
        System.out.println("Tempos acumulados por estado (N clientes no sistema):");

        for (int n = 0; n < r.temposEstados.length; n++) {
            double tempo = r.temposEstados[n];
            double prob = (r.tempoGlobal > 0.0) ? (tempo / r.tempoGlobal) : 0.0;
            System.out.printf(
                "Estado %d: tempo=%.6f, probabilidade=%.8f%n",
                n, tempo, prob
            );
        }
    }

    public static void main(String[] args) {
        ResultadoSimulacao gg15 = simular(1);
        ResultadoSimulacao gg25 = simular(2);

        imprimirResultado(gg15);
        imprimirResultado(gg25);
    }
}
