package dev.guilherme.notelogic.tarefa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O que o cliente pode mandar ao criar ou editar uma tarefa. */
public record TarefaRequest(
        @NotBlank @Size(max = 200) String titulo,
        @Size(max = 2000) String descricao) {
}
