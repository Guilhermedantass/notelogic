package dev.guilherme.notelogic.flashcard;

/**
 * Algoritmo SM-2 (SuperMemo 2, Piotr Woźniak, 1987), a base do agendamento do Anki.
 * Lógica pura: sem Spring, sem banco, sem relógio. Recebe o estado atual e a resposta e devolve o próximo estado.
 */
public final class AgendadorSm2 {

    static final double FACILIDADE_MINIMA = 1.3;

    private AgendadorSm2() {
    }

    public static EstadoAgendamento agendar(EstadoAgendamento atual, Resposta resposta) {
        int q = resposta.qualidade();

        // Errou: volta para o começo (o cartão reaparece ainda hoje) e a facilidade não muda
        if (q < 3) {
            return new EstadoAgendamento(0, 0, atual.facilidade());
        }

        // Acertou: "Difícil" baixa a facilidade (-0.14), "Bom" mantém, "Fácil" sobe (+0.10)
        double facilidade = atual.facilidade() + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        facilidade = Math.max(FACILIDADE_MINIMA, facilidade);

        int intervalo = switch (atual.repeticoes()) {
            case 0 -> 1;
            case 1 -> 6;
            default -> (int) Math.round(atual.intervaloDias() * facilidade);
        };

        return new EstadoAgendamento(atual.repeticoes() + 1, intervalo, facilidade);
    }
}
