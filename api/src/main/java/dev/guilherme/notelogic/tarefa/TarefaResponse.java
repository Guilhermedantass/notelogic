package dev.guilherme.notelogic.tarefa;

import java.time.Instant;

/** O que a API devolve sobre uma tarefa. Separado da entidade para o banco e o JSON evoluírem cada um no seu ritmo. */
public record TarefaResponse(Long id, String titulo, String descricao, boolean concluida, Instant criadaEm) {

    static TarefaResponse de(Tarefa tarefa) {
        return new TarefaResponse(tarefa.getId(), tarefa.getTitulo(), tarefa.getDescricao(),
                tarefa.isConcluida(), tarefa.getCriadaEm());
    }
}
