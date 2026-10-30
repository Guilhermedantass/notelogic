package dev.guilherme.notelogic.agenda;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import dev.guilherme.notelogic.materia.Materia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** O que ACONTECE num dia ou horário (prova, aula, treino). O que se conclui é Tarefa. */
@Entity
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(length = 200)
    private String local;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaEvento categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @Column(nullable = false)
    private LocalDate dataInicio;

    @Column(nullable = false)
    private LocalDate dataFim;

    @Column(nullable = false)
    private boolean diaInteiro;

    private LocalTime horaInicio;

    private LocalTime horaFim;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    protected Evento() {
        // exigido pelo JPA
    }

    public Evento(Long usuarioId, String titulo, String descricao, String local, CategoriaEvento categoria,
            Materia materia, PeriodoEvento periodo) {
        this.usuarioId = usuarioId;
        this.criadoEm = Instant.now();
        editar(titulo, descricao, local, categoria, materia, periodo);
    }

    public void editar(String titulo, String descricao, String local, CategoriaEvento categoria, Materia materia,
            PeriodoEvento periodo) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.local = local;
        this.categoria = categoria;
        this.materia = materia;
        this.dataInicio = periodo.dataInicio();
        this.dataFim = periodo.dataFim();
        this.diaInteiro = periodo.diaInteiro();
        this.horaInicio = periodo.horaInicio();
        this.horaFim = periodo.horaFim();
    }

    public PeriodoEvento periodo() {
        return new PeriodoEvento(dataInicio, dataFim, diaInteiro, horaInicio, horaFim);
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getLocal() {
        return local;
    }

    public CategoriaEvento getCategoria() {
        return categoria;
    }

    public Materia getMateria() {
        return materia;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
