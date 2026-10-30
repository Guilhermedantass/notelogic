package dev.guilherme.notelogic.agenda;

import java.time.LocalDate;

/**
 * Um evento como ele aparece num dia da agenda. Para um evento avulso é o próprio evento; para um que se repete,
 * é uma das datas da regra, já com a exceção ("só esta") aplicada.
 *
 * @param dataOcorrencia o dia ORIGINAL pela regra (identifica a ocorrência), ou null num evento avulso
 * @param alterada       true quando uma exceção mudou data, horário ou local
 */
public record OcorrenciaEvento(Evento evento, LocalDate dataOcorrencia, PeriodoEvento periodo, String local,
        boolean alterada) {

    static OcorrenciaEvento avulso(Evento evento) {
        return new OcorrenciaEvento(evento, null, evento.periodo(), evento.getLocal(), false);
    }
}
