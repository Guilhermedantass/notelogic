package dev.guilherme.notelogic.flashcard;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Uma resposta dada na tela de estudo. Só é gravada, nunca alterada. */
@Entity
public class Revisao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cartao_id")
    private Cartao cartao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Resposta resposta;

    /** O intervalo que o cartão ganhou com esta resposta. */
    @Column(nullable = false)
    private int intervaloDias;

    @Column(nullable = false, updatable = false)
    private Instant respondidaEm;

    protected Revisao() {
        // exigido pelo JPA
    }

    public Revisao(Cartao cartao, Resposta resposta, int intervaloDias, Instant respondidaEm) {
        this.cartao = cartao;
        this.resposta = resposta;
        this.intervaloDias = intervaloDias;
        this.respondidaEm = respondidaEm;
    }

    public Long getId() {
        return id;
    }

    public Resposta getResposta() {
        return resposta;
    }

    public Instant getRespondidaEm() {
        return respondidaEm;
    }
}
