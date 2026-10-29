package dev.guilherme.notelogic.materia;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** O "centro" do app: tarefas, baralhos e sessões de foco pertencem a uma matéria. */
@Entity
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    /** Cor em hexadecimal (#RRGGBB), usada nas etiquetas e gráficos do front. */
    @Column(nullable = false, length = 7)
    private String cor;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected Materia() {
        // exigido pelo JPA
    }

    public Materia(String nome, String cor) {
        this.nome = nome;
        this.cor = cor;
        this.criadaEm = Instant.now();
    }

    public void editar(String nome, String cor) {
        this.nome = nome;
        this.cor = cor;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCor() {
        return cor;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
