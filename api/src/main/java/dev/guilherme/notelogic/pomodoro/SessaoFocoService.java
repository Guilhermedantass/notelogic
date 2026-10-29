package dev.guilherme.notelogic.pomodoro;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.erro.ConflitoException;
import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;
import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.materia.MateriaService;
import dev.guilherme.notelogic.tarefa.Tarefa;
import dev.guilherme.notelogic.tarefa.TarefaService;

@Service
public class SessaoFocoService {

    private final SessaoFocoRepository repository;
    private final TarefaService tarefas;
    private final MateriaService materias;
    private final Clock clock;

    public SessaoFocoService(SessaoFocoRepository repository, TarefaService tarefas, MateriaService materias,
            Clock clock) {
        this.repository = repository;
        this.tarefas = tarefas;
        this.materias = materias;
        this.clock = clock;
    }

    @Transactional
    public SessaoFoco iniciar(TipoSessao tipo, Integer duracaoMin, Long tarefaId, Long materiaId) {
        // Checagem "amigável", com mensagem clara. O índice único do banco cobre a corrida entre duas requisições.
        if (repository.existsByStatus(StatusSessao.EM_ANDAMENTO)) {
            throw new ConflitoException("Já existe uma sessão em andamento");
        }
        Tarefa tarefa = tarefaId != null ? tarefas.buscar(tarefaId) : null;
        Materia materia = tarefa == null && materiaId != null ? materias.buscar(materiaId) : null;
        return repository.save(new SessaoFoco(tipo, duracaoMin, tarefa, materia, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Optional<SessaoFoco> atual() {
        return repository.findFirstByStatus(StatusSessao.EM_ANDAMENTO);
    }

    @Transactional
    public SessaoFoco concluir(Long id) {
        SessaoFoco sessao = buscar(id);
        sessao.concluir(clock.instant());
        return sessao;
    }

    @Transactional
    public SessaoFoco interromper(Long id) {
        SessaoFoco sessao = buscar(id);
        sessao.interromper(clock.instant());
        return sessao;
    }

    /** Sessões iniciadas entre os dias de e ate (inclusive), no fuso do app. */
    @Transactional(readOnly = true)
    public List<SessaoFoco> listar(LocalDate de, LocalDate ate) {
        Instant inicio = de.atStartOfDay(clock.getZone()).toInstant();
        Instant fim = ate.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
        return repository.findByIniciadaEmGreaterThanEqualAndIniciadaEmLessThanOrderByIniciadaEmDesc(inicio, fim);
    }

    public LocalDate hoje() {
        return LocalDate.now(clock);
    }

    private SessaoFoco buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Sessão", id));
    }
}
