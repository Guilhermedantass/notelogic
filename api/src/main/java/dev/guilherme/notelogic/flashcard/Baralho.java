package dev.guilherme.notelogic.flashcard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import dev.guilherme.notelogic.materia.Materia;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Baralho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    /** O lado "um" da relação. Apagar o baralho apaga os cartões (cascade + orphanRemoval). */
    @OneToMany(mappedBy = "baralho", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cartao> cartoes = new ArrayList<>();

    protected Baralho() {
        // exigido pelo JPA
    }

    public Baralho(String nome, String descricao, Materia materia) {
        this.criadoEm = Instant.now();
        editar(nome, descricao, materia);
    }

    public void editar(String nome, String descricao, Materia materia) {
        this.nome = nome;
        this.descricao = descricao;
        this.materia = materia;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public Materia getMateria() {
        return materia;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
