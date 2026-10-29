package dev.guilherme.notelogic.flashcard;

/**
 * O que o SM-2 sabe sobre um cartão.
 *
 * @param repeticoes    quantas vezes seguidas o cartão foi lembrado (zera quando você erra)
 * @param intervaloDias daqui a quantos dias ele volta
 * @param facilidade    o quanto o intervalo cresce a cada acerto (começa em 2.5, mínimo 1.3)
 */
public record EstadoAgendamento(int repeticoes, int intervaloDias, double facilidade) {

    public static final double FACILIDADE_INICIAL = 2.5;

    public static EstadoAgendamento novo() {
        return new EstadoAgendamento(0, 0, FACILIDADE_INICIAL);
    }
}
