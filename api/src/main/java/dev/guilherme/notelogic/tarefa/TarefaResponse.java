package dev.guilherme.notelogic.tarefa;

import java.time.Instant;
import java.time.LocalDate;

import dev.guilherme.notelogic.materia.MateriaResponse;
import dev.guilherme.notelogic.recorrencia.RecorrenciaDtos.RecorrenciaResumo;

/** O que a API devolve sobre uma tarefa. Separado da entidade para o banco e o JSON evoluírem cada um no seu ritmo. */
public record TarefaResponse(Long id, String titulo, String descricao, boolean concluida, Instant criadaEm,
        Instant concluidaEm, MateriaResponse materia, LocalDate prazo, Prioridade prioridade,
        RecorrenciaResumo recorrencia, LocalDate dataOcorrencia) {

    static TarefaResponse de(Tarefa tarefa) {
        MateriaResponse materia = tarefa.getMateria() != null ? MateriaResponse.de(tarefa.getMateria()) : null;
        RecorrenciaResumo recorrencia = tarefa.getRecorrencia() != null ? RecorrenciaResumo.de(tarefa.getRecorrencia()) : null;
        return new TarefaResponse(tarefa.getId(), tarefa.getTitulo(), tarefa.getDescricao(), tarefa.isConcluida(),
                tarefa.getCriadaEm(), tarefa.getConcluidaEm(), materia, tarefa.getPrazo(), tarefa.getPrioridade(),
                recorrencia, tarefa.getDataOcorrencia());
    }
}
