public class SimuladorFila {

    // ==========================================
    // PARÂMETROS DA SIMULAÇÃO (Configuração)
    // ==========================================
    static int capacidadeFila = 5;
    static int servidores = 1;          // Altere para 2 para testar a fila G/G/2/5
    static double chegadaMin = 2.0;
    static double chegadaMax = 5.0;
    static double atendimentoMin = 3.0;
    static double atendimentoMax = 5.0;
    static int limiteAleatorios = 100000;
    static double tempoPrimeiraChegada = 2.0;

    // ==========================================
    // PARÂMETROS DO GERADOR (LCG)
    // ==========================================
    // Usamos 'long' para evitar overflow na multiplicação (a * sementeAtual)
    static long m = 2147483647L; // 2^31 - 1
    static long a = 1103515245L;
    static long c = 12345L;
    static long sementeAtual = 42L; // Semente inicial (Seed)

    // Controlador de parada da simulação
    static int aleatoriosConsumidos = 0;

    /**
     * Função que gera o próximo número pseudoaleatório normalizado (entre 0 e 1).
     * Retorna null quando o limite de 100.000 números é atingido.
     */
    public static Double nextRandom() {
        if (aleatoriosConsumidos >= limiteAleatorios) {
            return null; // Sinaliza para o laço principal que a simulação acabou
        }

        // Aplica o Método Congruente Linear
        sementeAtual = (a * sementeAtual + c) % m;
        aleatoriosConsumidos++;

        // Retorna a probabilidade normalizada U
        return (double) sementeAtual / m;
    }

    public static Double calcularTempo(double min, double max) {
        Double u = nextRandom();

        // Se nextRandom retornou null, a simulação deve acabar. Repassamos o null para frente.
        if (u == null) {
            return null;
        }

        // Aplica a fórmula da Distribuição Uniforme Contínua: A + (B - A) * U
        return min + (max - min) * u;
    }

    // ==========================================
    // MÉTODO PRINCIPAL (Testes Iniciais)
    // ==========================================
    public static void main(String[] args) {
        System.out.println("Testando a geração de Tempos (Distribuição Uniforme)...");

        System.out.println("\nSimulando 3 tempos de CHEGADA (entre 2.0 e 5.0):");
        for (int i = 0; i < 3; i++) {
            Double tempoChegada = calcularTempo(chegadaMin, chegadaMax);
            System.out.printf("Chegada [%d]: %.4f unidades de tempo%n", (i+1), tempoChegada);
        }

        System.out.println("\nSimulando 3 tempos de ATENDIMENTO (entre 3.0 e 5.0):");
        for (int i = 0; i < 3; i++) {
            Double tempoAtendimento = calcularTempo(atendimentoMin, atendimentoMax);
            System.out.printf("Atendimento [%d]: %.4f unidades de tempo%n", (i+1), tempoAtendimento);
        }
    }
}