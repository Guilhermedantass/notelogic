package dev.guilherme.notelogic.erro;

/** Lançada pelos services quando um id não existe. O TratadorDeErros transforma em 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " " + id + " não encontrado(a)");
    }
}
