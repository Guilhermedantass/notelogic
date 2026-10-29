package dev.guilherme.notelogic.tarefa;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;
import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.materia.MateriaService;

@Service
public class TarefaService {

    private final TarefaRepository repository;
    private final MateriaService materias;

    public TarefaService(TarefaRepository repository, MateriaService materias) {
        this.repository = repository;
        this.materias = materias;
    }

    @Transactional(readOnly = true)
    public List<Tarefa> listar(Long materiaId, Boolean concluida) {
        return repository.filtrar(materiaId, concluida);
    }

    @Transactional(readOnly = true)
    public Tarefa buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa", id));
    }

    @Transactional
    public Tarefa criar(TarefaRequest dados) {
        Tarefa tarefa = new Tarefa(dados.titulo(), dados.descricao(), materiaDe(dados), dados.prazo(),
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
        tarefa.concluir(); // sem save: o Hibernate percebe a mudança e faz o UPDATE no fim da transação
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
