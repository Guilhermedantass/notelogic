package dev.guilherme.notelogic.pomodoro;

import java.time.Instant;

import dev.guilherme.notelogic.materia.MateriaResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class SessaoFocoDtos {

    private SessaoFocoDtos() {
    }

    /** Validação de FORMATO: tipo presente, duração num intervalo razoável. As regras de negócio ficam na entidade. */
    public record IniciarSessaoRequest(
            @NotNull TipoSessao tipo,
            @Min(1) @Max(180) Integer duracaoMin,
            Long tarefaId,
            Long materiaId) {
    }

    public record TarefaResumo(Long id, String titulo) {
    }

    public record SessaoResponse(Long id, TipoSessao tipo, StatusSessao status, int duracaoPlanejadaMin,
            Instant iniciadaEm, Instant fimPrevisto, Instant finalizadaEm, Integer segundosFocados,
            TarefaResumo tarefa, MateriaResponse materia) {

        static SessaoResponse de(SessaoFoco s) {
            TarefaResumo tarefa = s.getTarefa() != null
                    ? new TarefaResumo(s.getTarefa().getId(), s.getTarefa().getTitulo())
                    : null;
            MateriaResponse materia = s.getMateria() != null ? MateriaResponse.de(s.getMateria()) : null;
            return new SessaoResponse(s.getId(), s.getTipo(), s.getStatus(), s.getDuracaoPlanejadaMin(),
                    s.getIniciadaEm(), s.getFimPrevisto(), s.getFinalizadaEm(), s.getSegundosFocados(), tarefa,
                    materia);
        }
    }
}
