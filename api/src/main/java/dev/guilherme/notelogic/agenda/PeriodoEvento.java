package dev.guilherme.notelogic.agenda;

import java.time.LocalDate;
import java.time.LocalTime;

import dev.guilherme.notelogic.erro.RegraVioladaException;

/**
 * "Quando o evento acontece", sem nada de banco. As mesmas regras estão nos checks da V8.
 *
 * @param dataFim  igual a dataInicio num evento de um dia
 * @param horaFim  opcional: "prova às 14h" pode não ter fim
 */
public record PeriodoEvento(LocalDate dataInicio, LocalDate dataFim, boolean diaInteiro, LocalTime horaInicio,
        LocalTime horaFim) {

    /**
     * Monta o período a partir do que o cliente mandou: sem dataFim, o evento é de um dia; num evento de dia inteiro,
     * as horas são descartadas. Depois confere a coerência entre os campos.
     */
    public static PeriodoEvento de(LocalDate dataInicio, LocalDate dataFim, boolean diaInteiro, LocalTime horaInicio,
            LocalTime horaFim) {
        PeriodoEvento periodo = diaInteiro
                ? new PeriodoEvento(dataInicio, dataFim != null ? dataFim : dataInicio, true, null, null)
                : new PeriodoEvento(dataInicio, dataFim != null ? dataFim : dataInicio, false, horaInicio, horaFim);
        periodo.validar();
        return periodo;
    }

    private void validar() {
        exigir(!dataFim.isBefore(dataInicio), "O fim não pode ser antes do início");
        exigir(diaInteiro || horaInicio != null, "Informe a hora de início ou marque o evento como dia inteiro");
        exigir(horaFim == null || dataFim.isAfter(dataInicio) || horaFim.isAfter(horaInicio),
                "A hora de fim precisa ser depois da hora de início");
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new RegraVioladaException(mensagem);
        }
    }
}
