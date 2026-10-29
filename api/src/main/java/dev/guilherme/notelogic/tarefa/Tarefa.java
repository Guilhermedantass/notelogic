package dev.guilherme.notelogic.tarefa;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(nullable = false)
    private boolean concluida;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected Tarefa() {
        // exigido pelo JPA
    }

    public Tarefa(String titulo, String descricao) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.concluida = false;
        this.criadaEm = Instant.now();
    }

    public void concluir() {
        this.concluida = true;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isConcluida() {
        return concluida;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
