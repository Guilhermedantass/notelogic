package dev.guilherme.notelogic.erro;

/**
 * Os dados estão bem formados, mas a operação não faz sentido para o domínio
 * (ex.: uma pausa ligada a uma tarefa). Vira 422.
 */
public class RegraVioladaException extends RuntimeException {

    public RegraVioladaException(String mensagem) {
        super(mensagem);
    }
}
