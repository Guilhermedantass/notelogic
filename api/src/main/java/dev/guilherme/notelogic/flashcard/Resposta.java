package dev.guilherme.notelogic.flashcard;

/** Os quatro botões da tela de estudo, com a nota (0 a 5) que o SM-2 espera. */
public enum Resposta {
    ERREI(1),
    DIFICIL(3),
    BOM(4),
    FACIL(5);

    private final int qualidade;

    Resposta(int qualidade) {
        this.qualidade = qualidade;
    }

    public int qualidade() {
        return qualidade;
    }
}
