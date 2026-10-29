package dev.guilherme.notelogic.pomodoro;

/** Os três tempos clássicos do pomodoro, com a duração padrão de cada um. */
public enum TipoSessao {
    FOCO(25),
    PAUSA_CURTA(5),
    PAUSA_LONGA(15);

    private final int minutosPadrao;

    TipoSessao(int minutosPadrao) {
        this.minutosPadrao = minutosPadrao;
    }

    public int minutosPadrao() {
        return minutosPadrao;
    }

    public boolean ehPausa() {
        return this != FOCO;
    }
}
