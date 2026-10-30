package dev.guilherme.notelogic.agenda;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import dev.guilherme.notelogic.agenda.AgendaDtos.EventoResponse;

/**
 * O que a tela Hoje mostra além das tarefas: o que acontece hoje e o que vem aí de importante.
 *
 * @param agenda      os eventos de hoje, em ordem (dia inteiro primeiro, depois por horário)
 * @param importantes provas (categorias com destaque) e tarefas de prioridade alta nos próximos dias
 */
public record ResumoDoDia(LocalDate hoje, List<EventoResponse> agenda, List<DataImportante> importantes) {

    public enum Tipo {
        EVENTO, TAREFA
    }

    /**
     * Uma data importante. {@code diasAte}: 0 é hoje (ou um evento de vários dias que já começou).
     * {@code rotulo}: o nome da categoria do evento, ou "Tarefa".
     */
    public record DataImportante(Tipo tipo, Long id, String titulo, String rotulo, LocalDate data, LocalTime hora,
            String cor, long diasAte) {
    }
}
