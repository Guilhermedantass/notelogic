package dev.guilherme.notelogic.erro;

/** Lançada quando a operação bate de frente com algo que já existe (ex.: nome repetido). Vira 409. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
