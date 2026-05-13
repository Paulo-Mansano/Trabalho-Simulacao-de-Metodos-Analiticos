//===================================================================
// Andrei Böeck, Guilherme Tavares, Paulo Tavares e Thomaz Abrantes
//===================================================================

//=======================================
// Como executar:
// 1. Compile: javac SimuladorFila.java
// 2. Execute: java SimuladorFila
//=======================================

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SimuladorFila {

    // =========================
    // PARAMETROS DO LCG
    // =========================

    static long m = 2147483647L; // 2^31 - 1
    static long a = 1103515245L;
    static long c = 12345L;

    static long sementeAtual = 42L;
    static int aleatoriosConsumidos = 0;
    static int limiteAleatorios = 100000;

    // =========================
    // CONFIGURACOES LIDAS DO YAML
    // =========================

    static class ConfigModelo {
        String nomeArquivo;

        Map<String, Double> arrivals = new LinkedHashMap<>();
        Map<String, ConfigFila> filas = new LinkedHashMap<>();
        List<Rota> rotas = new ArrayList<>();

        int rndnumbersPerSeed = 100000;
        List<Long> seeds = new ArrayList<>();
    }

    static class ConfigFila {
        String id;

        int servidores;
        int capacidade;

        boolean capacidadeInfinita = false;

        Double minArrival = null;
        Double maxArrival = null;

        double minService;
        double maxService;
    }

    static class Rota {
        String origem;
        String destino;
        double probabilidade;

        Rota(String origem, String destino, double probabilidade) {
            this.origem = origem;
            this.destino = destino;
            this.probabilidade = probabilidade;
        }
    }

    // =========================
    // FILA EM TEMPO DE SIMULACAO
    // =========================

    static class Fila {
        String id;

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

        Fila(ConfigFila config) {
            this.id = config.id;
            this.capacidade = config.capacidade;
            this.servidores = config.servidores;
            this.atendimentoMin = config.minService;
            this.atendimentoMax = config.maxService;

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

    static class ResultadoSimulacao {
        String nomeModelo;
        double tempoGlobal;
        int aleatoriosUsados;
        int clientesQueSairamDoSistema;
        Map<String, Fila> filas = new LinkedHashMap<>();
    }

    // =========================
    // GERADOR ALEATORIO
    // =========================

    public static void resetGerador(long seed, int limite) {
        sementeAtual = seed;
        aleatoriosConsumidos = 0;
        limiteAleatorios = limite;
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
    // LEITOR YAML SIMPLES
    // =========================

    public static ConfigModelo lerYaml(String caminhoArquivo) throws IOException {
        ConfigModelo config = new ConfigModelo();
        config.nomeArquivo = caminhoArquivo;

        BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo));

        String secaoAtual = "";
        ConfigFila filaAtual = null;
        Rota rotaAtual = null;

        String linha;

        while ((linha = br.readLine()) != null) {
            String limpa = linha.trim();

            if (limpa.isEmpty()) {
                continue;
            }

            if (limpa.startsWith("#")) {
                continue;
            }

            if (limpa.startsWith("!PARAMETERS")) {
                continue;
            }

            if (limpa.equals("arrivals:")) {
                secaoAtual = "arrivals";
                filaAtual = null;
                rotaAtual = null;
                continue;
            }

            if (limpa.equals("queues:")) {
                secaoAtual = "queues";
                filaAtual = null;
                rotaAtual = null;
                continue;
            }

            if (limpa.equals("network:")) {
                secaoAtual = "network";
                filaAtual = null;
                rotaAtual = null;
                continue;
            }

            if (limpa.equals("seeds:")) {
                secaoAtual = "seeds";
                filaAtual = null;
                rotaAtual = null;
                continue;
            }

            if (limpa.equals("rndnumbers:")) {
                secaoAtual = "rndnumbers";
                filaAtual = null;
                rotaAtual = null;
                continue;
            }

            if (limpa.startsWith("rndnumbersPerSeed:")) {
                String valor = limpa.split(":")[1].trim();
                config.rndnumbersPerSeed = Integer.parseInt(valor);
                continue;
            }

            if (secaoAtual.equals("arrivals")) {
                if (limpa.contains(":")) {
                    String[] partes = limpa.split(":");
                    String fila = partes[0].trim();
                    double tempo = Double.parseDouble(partes[1].trim());
                    config.arrivals.put(fila, tempo);
                }
                continue;
            }

            if (secaoAtual.equals("queues")) {
                if (limpa.matches("Q[0-9A-Za-z_]+:")) {
                    String idFila = limpa.replace(":", "").trim();

                    filaAtual = new ConfigFila();
                    filaAtual.id = idFila;

                    config.filas.put(idFila, filaAtual);
                    continue;
                }

                if (filaAtual != null && limpa.contains(":")) {
                    String[] partes = limpa.split(":");
                    String chave = partes[0].trim();
                    String valor = partes[1].trim();

                    if (chave.equals("servers")) {
                        filaAtual.servidores = Integer.parseInt(valor);
                    } else if (chave.equals("capacity")) {
                        filaAtual.capacidade = Integer.parseInt(valor);
                    } else if (chave.equals("minArrival")) {
                        filaAtual.minArrival = Double.parseDouble(valor);
                    } else if (chave.equals("maxArrival")) {
                        filaAtual.maxArrival = Double.parseDouble(valor);
                    } else if (chave.equals("minService")) {
                        filaAtual.minService = Double.parseDouble(valor);
                    } else if (chave.equals("maxService")) {
                        filaAtual.maxService = Double.parseDouble(valor);
                    }
                }

                continue;
            }

            if (secaoAtual.equals("network")) {
                if (limpa.startsWith("-")) {
                    String semTraco = limpa.substring(1).trim();

                    if (semTraco.startsWith("source:")) {
                        String origem = semTraco.split(":")[1].trim();
                        rotaAtual = new Rota(origem, "", 0.0);
                    }

                    continue;
                }

                if (rotaAtual != null && limpa.startsWith("source:")) {
                    rotaAtual.origem = limpa.split(":")[1].trim();
                    continue;
                }

                if (rotaAtual != null && limpa.startsWith("target:")) {
                    rotaAtual.destino = limpa.split(":")[1].trim();
                    continue;
                }

                if (rotaAtual != null && limpa.startsWith("probability:")) {
                    rotaAtual.probabilidade = Double.parseDouble(limpa.split(":")[1].trim());
                    config.rotas.add(rotaAtual);
                    rotaAtual = null;
                }

                continue;
            }

            if (secaoAtual.equals("seeds")) {
                if (limpa.startsWith("-")) {
                    String valor = limpa.substring(1).trim();
                    config.seeds.add(Long.parseLong(valor));
                }
            }
        }

        br.close();

        for (ConfigFila fila : config.filas.values()) {
            if (fila.capacidade <= 0) {
                fila.capacidade = 100;
                fila.capacidadeInfinita = true;
            }
        }

        if (config.seeds.isEmpty()) {
            config.seeds.add(42L);
        }

        return config;
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

    public static void atualizarEstatisticasTempo(Map<String, Fila> filas, double deltaTempo) {
        for (Fila fila : filas.values()) {
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

    public static String escolherDestino(ConfigModelo config, String origem) {
        List<Rota> rotasOrigem = new ArrayList<>();

        for (Rota rota : config.rotas) {
            if (rota.origem.equals(origem)) {
                rotasOrigem.add(rota);
            }
        }

        if (rotasOrigem.isEmpty()) {
            return null;
        }

        Double u = nextRandom();

        if (u == null) {
            return null;
        }

        double acumulado = 0.0;

        for (Rota rota : rotasOrigem) {
            acumulado += rota.probabilidade;

            if (u < acumulado) {
                return rota.destino;
            }
        }

        return null;
    }

    // =========================
    // SIMULACAO
    // =========================

    public static ResultadoSimulacao simular(ConfigModelo config, String nomeModelo) {
        long seed = config.seeds.get(0);
        resetGerador(seed, config.rndnumbersPerSeed);

        double tempoAtual = 0.0;

        Map<String, Fila> filas = new LinkedHashMap<>();

        for (ConfigFila configFila : config.filas.values()) {
            filas.put(configFila.id, new Fila(configFila));
        }

        Map<String, Double> proximasChegadasExternas = new LinkedHashMap<>();

        for (Map.Entry<String, Double> entrada : config.arrivals.entrySet()) {
            proximasChegadasExternas.put(entrada.getKey(), entrada.getValue());
        }

        int clientesQueSairamDoSistema = 0;

        boolean encerrar = false;

        while (!encerrar) {
            String filaChegadaExterna = null;
            double tempoProximaChegadaExterna = Double.POSITIVE_INFINITY;

            for (Map.Entry<String, Double> entrada : proximasChegadasExternas.entrySet()) {
                if (entrada.getValue() < tempoProximaChegadaExterna) {
                    tempoProximaChegadaExterna = entrada.getValue();
                    filaChegadaExterna = entrada.getKey();
                }
            }

            Fila filaProximaSaida = null;
            int indiceServidorSaida = -1;
            double tempoProximaSaida = Double.POSITIVE_INFINITY;

            for (Fila fila : filas.values()) {
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

            double proximoEvento = Math.min(tempoProximaChegadaExterna, tempoProximaSaida);

            if (Double.isInfinite(proximoEvento)) {
                break;
            }

            double deltaTempo = proximoEvento - tempoAtual;
            atualizarEstatisticasTempo(filas, deltaTempo);
            tempoAtual = proximoEvento;

            if (tempoProximaSaida <= tempoProximaChegadaExterna) {
                Fila fila = filaProximaSaida;

                fila.saidasServidores[indiceServidorSaida] = Double.POSITIVE_INFINITY;
                fila.clientesNoSistema--;
                fila.servidoresOcupados--;
                fila.atendidos++;

                boolean ok = iniciarAtendimentoSePossivel(fila, tempoAtual);

                if (!ok) {
                    encerrar = true;
                    continue;
                }

                String destino = escolherDestino(config, fila.id);

                if (destino == null) {
                    clientesQueSairamDoSistema++;
                } else {
                    Fila filaDestino = filas.get(destino);

                    if (filaDestino != null) {
                        ok = chegadaNaFila(filaDestino, tempoAtual);

                        if (!ok) {
                            encerrar = true;
                        }
                    } else {
                        clientesQueSairamDoSistema++;
                    }
                }

            } else {
                Fila filaDestino = filas.get(filaChegadaExterna);

                if (filaDestino != null) {
                    boolean ok = chegadaNaFila(filaDestino, tempoAtual);

                    if (!ok) {
                        encerrar = true;
                        continue;
                    }

                    ConfigFila configFila = config.filas.get(filaChegadaExterna);

                    if (configFila.minArrival != null && configFila.maxArrival != null) {
                        Double tempoEntreChegadas = calcularTempo(configFila.minArrival, configFila.maxArrival);

                        if (tempoEntreChegadas == null) {
                            proximasChegadasExternas.put(filaChegadaExterna, Double.POSITIVE_INFINITY);
                            encerrar = true;
                        } else {
                            proximasChegadasExternas.put(filaChegadaExterna, tempoAtual + tempoEntreChegadas);
                        }
                    } else {
                        proximasChegadasExternas.put(filaChegadaExterna, Double.POSITIVE_INFINITY);
                    }
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

        for (Fila fila : r.filas.values()) {
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

    // =========================
    // IMPRESSAO
    // =========================

    public static void imprimirResultado(ResultadoSimulacao r) {
        System.out.println();
        System.out.println("==========================================================");
        System.out.println("RESULTADO DA SIMULACAO: " + r.nomeModelo);
        System.out.println("==========================================================");

        System.out.printf("Tempo global da simulacao: %.6f minutos%n", r.tempoGlobal);
        System.out.printf("Aleatorios usados: %d%n", r.aleatoriosUsados);
        System.out.printf("Clientes que sairam do sistema: %d%n", r.clientesQueSairamDoSistema);

        System.out.println();
        System.out.println("INDICES DE DESEMPENHO:");
        System.out.println("----------------------------------------------------------");

        System.out.printf(
                "%-10s %12s %12s %12s %16s %10s%n",
                "Fila",
                "Pop.Media",
                "Vazao",
                "Utilizacao",
                "Tempo Resp.",
                "Perdas"
        );

        for (Fila fila : r.filas.values()) {
            double populacaoMedia = calcularPopulacaoMedia(fila, r.tempoGlobal);
            double vazao = calcularVazao(fila, r.tempoGlobal);
            double utilizacao = calcularUtilizacao(fila, r.tempoGlobal);
            double tempoResposta = calcularTempoResposta(populacaoMedia, vazao);

            System.out.printf(
                    "%-10s %12.6f %12.6f %11.2f%% %16.6f %10d%n",
                    fila.id,
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

        for (Fila fila : r.filas.values()) {
            System.out.println();
            System.out.println("Fila: " + fila.id);

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
                "%-10s %16s %16s %20s%n",
                "Fila",
                "R Inicial",
                "R Melhorado",
                "Variacao (%)"
        );

        for (String idFila : inicial.filas.keySet()) {
            Fila filaInicial = inicial.filas.get(idFila);
            Fila filaMelhorada = melhorado.filas.get(idFila);

            if (filaMelhorada == null) {
                continue;
            }

            double nInicial = calcularPopulacaoMedia(filaInicial, inicial.tempoGlobal);
            double xInicial = calcularVazao(filaInicial, inicial.tempoGlobal);
            double rInicial = calcularTempoResposta(nInicial, xInicial);

            double nMelhorado = calcularPopulacaoMedia(filaMelhorada, melhorado.tempoGlobal);
            double xMelhorado = calcularVazao(filaMelhorada, melhorado.tempoGlobal);
            double rMelhorado = calcularTempoResposta(nMelhorado, xMelhorado);

            double variacaoPercentual = 0.0;

            if (rInicial != 0.0) {
                variacaoPercentual = ((rMelhorado - rInicial) / rInicial) * 100.0;
            }

            System.out.printf(
                    "%-10s %16.6f %16.6f %+19.2f%%%n",
                    idFila,
                    rInicial,
                    rMelhorado,
                    variacaoPercentual
            );
        }

        int perdasInicial = calcularPerdasTotais(inicial);
        int perdasMelhorado = calcularPerdasTotais(melhorado);

        double variacaoPerdasPercentual = 0.0;

        if (perdasInicial != 0) {
            variacaoPerdasPercentual = ((double) (perdasMelhorado - perdasInicial) / perdasInicial) * 100.0;
        }

        double taxaPerdasInicial = calcularTaxaPerdas(inicial);
        double taxaPerdasMelhorado = calcularTaxaPerdas(melhorado);

        double variacaoTaxaPerdasPercentual = 0.0;

        if (taxaPerdasInicial != 0.0) {
            variacaoTaxaPerdasPercentual =
                    ((taxaPerdasMelhorado - taxaPerdasInicial) / taxaPerdasInicial) * 100.0;
        }

        System.out.println();
        System.out.printf("Perdas totais no modelo inicial: %d%n", perdasInicial);
        System.out.printf("Perdas totais no modelo melhorado: %d%n", perdasMelhorado);
        System.out.printf("Variacao das perdas totais: %+.2f%%%n", variacaoPerdasPercentual);

        System.out.println();
        System.out.printf("Taxa de perdas inicial: %.8f perdas/min%n", taxaPerdasInicial);
        System.out.printf("Taxa de perdas melhorada: %.8f perdas/min%n", taxaPerdasMelhorado);
        System.out.printf("Variacao da taxa de perdas: %+.2f%%%n", variacaoTaxaPerdasPercentual);
    }

    // =========================
    // MAIN
    // =========================

    public static void main(String[] args) {
        try {
            String arquivoInicial = "modelo_inicial.yml";
            String arquivoMelhorado = "modelo_melhorado.yml";

            if (args.length >= 2) {
                arquivoInicial = args[0];
                arquivoMelhorado = args[1];
            }

            ConfigModelo configInicial = lerYaml(arquivoInicial);
            ConfigModelo configMelhorado = lerYaml(arquivoMelhorado);

            ResultadoSimulacao modeloInicial = simular(configInicial, "Modelo Inicial - " + arquivoInicial);
            ResultadoSimulacao modeloMelhorado = simular(configMelhorado, "Modelo Melhorado - " + arquivoMelhorado);

            imprimirResultado(modeloInicial);
            imprimirResultado(modeloMelhorado);
            imprimirComparacao(modeloInicial, modeloMelhorado);

        } catch (IOException e) {
            System.out.println("Erro ao ler arquivo YAML: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erro durante a simulacao: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
