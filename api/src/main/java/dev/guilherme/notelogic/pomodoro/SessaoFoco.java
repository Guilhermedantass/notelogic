package dev.guilherme.notelogic.pomodoro;

import java.time.Duration;
import java.time.Instant;

import dev.guilherme.notelogic.erro.ConflitoException;
import dev.guilherme.notelogic.erro.RegraVioladaException;
import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.tarefa.Tarefa;
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

/**
 * Um bloco de tempo do pomodoro. As regras de transição de status ficam aqui dentro:
 * ninguém de fora consegue, por exemplo, concluir uma sessão já interrompida.
 */
@Entity
public class SessaoFoco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dono do dado. Só o id (e não um @ManyToOne Usuario): nenhuma tela precisa carregar o usuário junto. */
    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoSessao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusSessao status;

    @Column(nullable = false)
    private int duracaoPlanejadaMin;

    @Column(nullable = false, updatable = false)
    private Instant iniciadaEm;

    private Instant finalizadaEm;

    /** Quanto tempo de fato contou: nunca passa da duração planejada. */
    private Integer segundosFocados;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarefa_id")
    private Tarefa tarefa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    protected SessaoFoco() {
        // exigido pelo JPA
    }

    /**
     * Se vier uma tarefa, a matéria é a dela (a sessão "herda" a matéria).
     * Pausas não pertencem a nada: são descanso.
     */
    public SessaoFoco(Long usuarioId, TipoSessao tipo, Integer duracaoMin, Tarefa tarefa, Materia materia,
            Instant agora) {
        if (tipo.ehPausa() && (tarefa != null || materia != null)) {
            throw new RegraVioladaException("Pausas não podem ser ligadas a uma tarefa ou matéria");
        }
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.status = StatusSessao.EM_ANDAMENTO;
        this.duracaoPlanejadaMin = duracaoMin != null ? duracaoMin : tipo.minutosPadrao();
        this.iniciadaEm = agora;
        this.tarefa = tarefa;
        this.materia = tarefa != null ? tarefa.getMateria() : materia;
    }

    public void concluir(Instant agora) {
        finalizar(StatusSessao.CONCLUIDA, agora);
    }

    public void interromper(Instant agora) {
        finalizar(StatusSessao.INTERROMPIDA, agora);
    }

    private void finalizar(StatusSessao novoStatus, Instant agora) {
        if (status != StatusSessao.EM_ANDAMENTO) {
            throw new ConflitoException("A sessão " + id + " já foi finalizada");
        }
        long decorridos = Duration.between(iniciadaEm, agora).toSeconds();
        this.segundosFocados = (int) Math.min(decorridos, duracaoPlanejadaMin * 60L);
        this.status = novoStatus;
        this.finalizadaEm = agora;
    }

    public Instant getFimPrevisto() {
        return iniciadaEm.plus(Duration.ofMinutes(duracaoPlanejadaMin));
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public TipoSessao getTipo() {
        return tipo;
    }

    public StatusSessao getStatus() {
        return status;
    }

    public int getDuracaoPlanejadaMin() {
        return duracaoPlanejadaMin;
    }

    public Instant getIniciadaEm() {
        return iniciadaEm;
    }

    public Instant getFinalizadaEm() {
        return finalizadaEm;
    }

    public Integer getSegundosFocados() {
        return segundosFocados;
    }

    public Tarefa getTarefa() {
        return tarefa;
    }

    public Materia getMateria() {
        return materia;
    }
}
