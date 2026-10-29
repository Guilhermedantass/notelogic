package dev.guilherme.notelogic.erro;

/** E-mail ou senha errados. Vira 401, sem dizer qual dos dois falhou. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha incorretos");
    }
}
