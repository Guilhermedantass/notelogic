package dev.guilherme.notelogic.tarefa;

import java.time.Instant;

public record Tarefa(Long id, String titulo, String descricao, boolean concluida, Instant criadaEm) {

    public Tarefa concluir() {
        return new Tarefa(id, titulo, descricao, true, criadaEm);
    }
}
