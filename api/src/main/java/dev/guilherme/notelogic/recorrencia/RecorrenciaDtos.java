package dev.guilherme.notelogic.recorrencia;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import dev.guilherme.notelogic.materia.MateriaResponse;
import dev.guilherme.notelogic.tarefa.Prioridade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RecorrenciaDtos {

    private RecorrenciaDtos() {
    }

    /** Formato de cada campo. A coerência entre eles (SEMANAL precisa de dias...) fica em RegraRecorrencia.validar(). */
    public record RegraRequest(
            @NotNull TipoRecorrencia tipo,
            Set<DayOfWeek> diasSemana,
            @Min(1) @Max(365) Integer intervaloDias,
            @Min(1) @Max(31) Integer diaDoMes,
            LocalDate inicio,
            LocalDate fim,
            @Min(1) Integer maxOcorrencias) {

        RegraRecorrencia paraRegra(LocalDate hoje) {
            return new RegraRecorrencia(tipo, diasSemana, intervaloDias, diaDoMes, inicio != null ? inicio : hoje, fim,
                    maxOcorrencias);
        }
    }

    public record RecorrenciaRequest(
            @NotBlank @Size(max = 200) String titulo,
            @Size(max = 2000) String descricao,
            Long materiaId,
            Prioridade prioridade,
            @NotNull @Valid RegraRequest regra,
            Boolean acumula) {

        /** Opcional no JSON: sem o campo, vale o padrão "expira". (Um boolean primitivo faria o Jackson recusar o JSON.) */
        boolean acumulaOuPadrao() {
            return Boolean.TRUE.equals(acumula);
        }
    }

    /** O resumo que acompanha cada tarefa que é ocorrência (ícone de repetir + "Toda segunda"). */
    public record RecorrenciaResumo(Long id, String descricao, boolean acumula) {

        public static RecorrenciaResumo de(Recorrencia r) {
            return new RecorrenciaResumo(r.getId(), CalendarioRecorrencia.descrever(r.regra()), r.isAcumula());
        }
    }

    /**
     * A regra completa, com a constância: {@code feitas} de {@code esperadas} vezes, do início até hoje.
     */
    public record RecorrenciaResponse(Long id, String titulo, String descricao, MateriaResponse materia,
            Prioridade prioridade, RegraRecorrencia regra, String resumo, boolean acumula, boolean encerrada,
            long feitas, long esperadas) {
    }
}
