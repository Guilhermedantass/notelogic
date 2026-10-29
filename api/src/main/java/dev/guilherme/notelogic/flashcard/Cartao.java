package dev.guilherme.notelogic.flashcard;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "baralho_id")
    private Baralho baralho;

    @Column(nullable = false, length = 1000)
    private String frente;

    @Column(nullable = false, length = 2000)
    private String verso;

    @Column(nullable = false)
    private int repeticoes;

    @Column(nullable = false)
    private int intervaloDias;

    @Column(nullable = false)
    private double facilidade;

    @Column(nullable = false)
    private LocalDate proximaRevisao;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    protected Cartao() {
        // exigido pelo JPA
    }

    /** Cartão novo já entra na fila de hoje. */
    public Cartao(Baralho baralho, String frente, String verso, LocalDate hoje) {
        this.baralho = baralho;
        this.frente = frente;
        this.verso = verso;
        this.criadoEm = Instant.now();
        this.proximaRevisao = hoje;
        aplicar(EstadoAgendamento.novo());
    }

    public void editar(String frente, String verso) {
        this.frente = frente;
        this.verso = verso;
    }

    /** Registra a resposta do estudo e reagenda o cartão a partir de hoje. */
    public void responder(Resposta resposta, LocalDate hoje) {
        EstadoAgendamento novo = AgendadorSm2.agendar(estado(), resposta);
        aplicar(novo);
        this.proximaRevisao = hoje.plusDays(novo.intervaloDias());
    }

    public EstadoAgendamento estado() {
        return new EstadoAgendamento(repeticoes, intervaloDias, facilidade);
    }

    private void aplicar(EstadoAgendamento estado) {
        this.repeticoes = estado.repeticoes();
        this.intervaloDias = estado.intervaloDias();
        this.facilidade = estado.facilidade();
    }

    public Long getId() {
        return id;
    }

    public Baralho getBaralho() {
        return baralho;
    }

    public String getFrente() {
        return frente;
    }

    public String getVerso() {
        return verso;
    }

    public LocalDate getProximaRevisao() {
        return proximaRevisao;
    }
}
