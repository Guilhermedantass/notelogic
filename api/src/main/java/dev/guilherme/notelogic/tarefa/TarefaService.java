package dev.guilherme.notelogic.tarefa;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;
import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.materia.MateriaService;
import dev.guilherme.notelogic.usuario.UsuarioAtual;

@Service
public class TarefaService {

    private final TarefaRepository repository;
    private final MateriaService materias;
    private final Clock clock;
    private final UsuarioAtual usuario;

    public TarefaService(TarefaRepository repository, MateriaService materias, Clock clock, UsuarioAtual usuario) {
        this.repository = repository;
        this.materias = materias;
        this.clock = clock;
        this.usuario = usuario;
    }

    @Transactional(readOnly = true)
    public List<Tarefa> listar(Long materiaId, Boolean concluida) {
        return repository.filtrar(usuario.id(), materiaId, concluida);
    }

    @Transactional(readOnly = true)
    public Tarefa buscar(Long id) {
        return repository.findByIdAndUsuarioId(id, usuario.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa", id));
    }

    @Transactional
    public Tarefa criar(TarefaRequest dados) {
        Tarefa tarefa = new Tarefa(usuario.id(), dados.titulo(), dados.descricao(), materiaDe(dados), dados.prazo(),
                dados.prioridade());
        return repository.save(tarefa);
    }

    @Transactional
    public Tarefa editar(Long id, TarefaRequest dados) {
        Tarefa tarefa = buscar(id);
        tarefa.editar(dados.titulo(), dados.descricao(), materiaDe(dados), dados.prazo(), dados.prioridade());
        return tarefa;
    }

    @Transactional
    public Tarefa concluir(Long id) {
        Tarefa tarefa = buscar(id);
        tarefa.concluir(clock.instant()); // sem save: o Hibernate percebe a mudança e faz o UPDATE no fim da transação
        return tarefa;
    }

    @Transactional
    public Tarefa reabrir(Long id) {
        Tarefa tarefa = buscar(id);
        tarefa.reabrir();
        return tarefa;
    }

    @Transactional
    public void remover(Long id) {
        repository.delete(buscar(id));
    }

    private Materia materiaDe(TarefaRequest dados) {
        return dados.materiaId() != null ? materias.buscar(dados.materiaId()) : null;
    }
}
