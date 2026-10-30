package dev.guilherme.notelogic.agenda;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Como a pessoa agrupa os eventos (Prova, Aula, Treino...). Dá a cor do evento na agenda. */
@Entity
@Table(name = "categoria_evento")
public class CategoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 50)
    private String nome;

    /** Cor em hexadecimal (#RRGGBB). */
    @Column(nullable = false, length = 7)
    private String cor;

    /** Os eventos desta categoria aparecem em "próximas datas importantes" no Hoje. */
    @Column(nullable = false)
    private boolean destaque;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected CategoriaEvento() {
        // exigido pelo JPA
    }

    public CategoriaEvento(Long usuarioId, String nome, String cor, boolean destaque) {
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.cor = cor;
        this.destaque = destaque;
        this.criadaEm = Instant.now();
    }

    public void editar(String nome, String cor, boolean destaque) {
        this.nome = nome;
        this.cor = cor;
        this.destaque = destaque;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getNome() {
        return nome;
    }

    public String getCor() {
        return cor;
    }

    public boolean isDestaque() {
        return destaque;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
