package dev.guilherme.notelogic.recorrencia;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.tarefa.Prioridade;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Uma tarefa que se repete: o "molde" (título, matéria, prioridade) e a regra de quando. */
@Entity
public class Recorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    // ---- o molde das ocorrências ----

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    // ---- a regra ----

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoRecorrencia tipo;

    @Convert(converter = DiasDaSemanaConverter.class)
    @Column(length = 60)
    private Set<DayOfWeek> diasSemana = EnumSet.noneOf(DayOfWeek.class);

    private Integer intervaloDias;

    private Integer diaDoMes;

    @Column(nullable = false)
    private LocalDate inicio;

    private LocalDate fim;

    private Integer maxOcorrencias;

    /** false = "expira" (padrão): não fez no dia, passou. true = "acumula": vira atrasada. */
    @Column(nullable = false)
    private boolean acumula;

    /** Até que dia as ocorrências já foram geradas (nulo = nenhuma ainda). */
    private LocalDate ultimaGerada;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected Recorrencia() {
        // exigido pelo JPA
    }

    public Recorrencia(Long usuarioId, String titulo, String descricao, Materia materia, Prioridade prioridade,
            RegraRecorrencia regra, boolean acumula) {
        this.usuarioId = usuarioId;
        this.criadaEm = Instant.now();
        editar(titulo, descricao, materia, prioridade, regra, acumula);
    }

    public void editar(String titulo, String descricao, Materia materia, Prioridade prioridade, RegraRecorrencia regra,
            boolean acumula) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.materia = materia;
        this.prioridade = prioridade != null ? prioridade : Prioridade.MEDIA;
        this.tipo = regra.tipo();
        this.diasSemana = regra.diasSemana().isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(regra.diasSemana());
        this.intervaloDias = regra.intervaloDias();
        this.diaDoMes = regra.diaDoMes();
        this.inicio = regra.inicio();
        this.fim = regra.fim();
        this.maxOcorrencias = regra.maxOcorrencias();
        this.acumula = acumula;
    }

    /** A regra para de valer depois de {@code ultimoDia}. */
    public void encerrarEm(LocalDate ultimoDia) {
        this.fim = ultimoDia;
    }

    public void marcarGeradaAte(LocalDate dia) {
        this.ultimaGerada = dia;
    }

    public RegraRecorrencia regra() {
        return new RegraRecorrencia(tipo, diasSemana, intervaloDias, diaDoMes, inicio, fim, maxOcorrencias);
    }

    public boolean encerrada(LocalDate hoje) {
        return fim != null && fim.isBefore(hoje);
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

    public Materia getMateria() {
        return materia;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public boolean isAcumula() {
        return acumula;
    }

    public LocalDate getUltimaGerada() {
        return ultimaGerada;
    }
}
