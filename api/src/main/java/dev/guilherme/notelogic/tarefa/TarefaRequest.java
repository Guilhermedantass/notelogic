package dev.guilherme.notelogic.tarefa;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O que o cliente pode mandar ao criar ou editar uma tarefa. Só o título é obrigatório. */
public record TarefaRequest(
        @NotBlank @Size(max = 200) String titulo,
        @Size(max = 2000) String descricao,
        Long materiaId,
        LocalDate prazo,
        Prioridade prioridade) {
}
