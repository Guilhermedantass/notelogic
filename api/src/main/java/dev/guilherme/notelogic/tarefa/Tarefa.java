package dev.guilherme.notelogic.tarefa;

import java.time.Instant;
import java.time.LocalDate;

import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.recorrencia.Recorrencia;
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

@Entity
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dono do dado. Só o id (e não um @ManyToOne Usuario): nenhuma tela precisa carregar o usuário junto. */
    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(nullable = false)
    private boolean concluida;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    /** Quando foi concluída pela última vez (nulo enquanto pendente). Alimenta as estatísticas. */
    private Instant concluidaEm;

    /** Opcional: uma tarefa pode não ser de matéria nenhuma. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    /** Um dia do calendário, sem hora nem fuso: "entregar até 05/11". */
    private LocalDate prazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    /** Se esta tarefa é a ocorrência de uma tarefa que se repete, a regra e o dia dela. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorrencia_id")
    private Recorrencia recorrencia;

    private LocalDate dataOcorrencia;

    /** Ocorrência que passou do dia sem ser feita (regras "expira"): some das listas, fica no histórico. */
    @Column(nullable = false)
    private boolean expirada;

    protected Tarefa() {
        // exigido pelo JPA
    }

    public Tarefa(Long usuarioId, String titulo, String descricao, Materia materia, LocalDate prazo,
            Prioridade prioridade) {
        this.usuarioId = usuarioId;
        this.criadaEm = Instant.now();
        this.concluida = false;
        editar(titulo, descricao, materia, prazo, prioridade);
    }

    /** A tarefa do dia {@code data} de uma regra que se repete: copia o molde da regra. */
    public static Tarefa ocorrencia(Recorrencia regra, LocalDate data) {
        Tarefa t = new Tarefa(regra.getUsuarioId(), regra.getTitulo(), regra.getDescricao(), regra.getMateria(), data,
                regra.getPrioridade());
        t.recorrencia = regra;
        t.dataOcorrencia = data;
        return t;
    }

    public void editar(String titulo, String descricao, Materia materia, LocalDate prazo, Prioridade prioridade) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.materia = materia;
        this.prazo = prazo;
        this.prioridade = prioridade != null ? prioridade : Prioridade.MEDIA;
    }

    public void concluir(Instant agora) {
        this.concluida = true;
        this.concluidaEm = agora;
    }

    public void reabrir() {
        this.concluida = false;
        this.concluidaEm = null;
    }

    public Long getId() {
        return id;
    }

    public Recorrencia getRecorrencia() {
        return recorrencia;
    }

    public LocalDate getDataOcorrencia() {
        return dataOcorrencia;
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

    public boolean isConcluida() {
        return concluida;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getConcluidaEm() {
        return concluidaEm;
    }

    public Materia getMateria() {
        return materia;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }
}
