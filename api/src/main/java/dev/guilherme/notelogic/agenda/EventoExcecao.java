package dev.guilherme.notelogic.agenda;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * "Só esta": num evento que se repete, a ocorrência de um dia foi cancelada ou mudou de data, horário ou local.
 * Guarda a ocorrência como ficou, inteira (e não só o que mudou).
 */
@Entity
@Table(name = "evento_excecao")
public class EventoExcecao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, updatable = false)
    private Long eventoId;

    /** O dia ORIGINAL da ocorrência, pela regra. É por ele que a exceção é encontrada. */
    @Column(nullable = false, updatable = false)
    private LocalDate data;

    @Column(nullable = false)
    private boolean cancelada;

    private LocalDate novaData;

    private LocalTime horaInicio;

    private LocalTime horaFim;

    @Column(length = 200)
    private String local;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected EventoExcecao() {
        // exigido pelo JPA
    }

    public EventoExcecao(Long eventoId, LocalDate data) {
        this.eventoId = eventoId;
        this.data = data;
        this.criadaEm = Instant.now();
    }

    public void cancelar() {
        this.cancelada = true;
        this.novaData = null;
        this.horaInicio = null;
        this.horaFim = null;
        this.local = null;
    }

    /** {@code periodo} é de um dia só (eventos de vários dias não se repetem). */
    public void alterar(PeriodoEvento periodo, String local) {
        this.cancelada = false;
        this.novaData = periodo.dataInicio();
        this.horaInicio = periodo.horaInicio();
        this.horaFim = periodo.horaFim();
        this.local = local;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public LocalDate getData() {
        return data;
    }

    public boolean isCancelada() {
        return cancelada;
    }

    public LocalDate getNovaData() {
        return novaData;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public String getLocal() {
        return local;
    }
}
