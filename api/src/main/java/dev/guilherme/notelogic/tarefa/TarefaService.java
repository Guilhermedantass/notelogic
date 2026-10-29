package dev.guilherme.notelogic.tarefa;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;

@Service
public class TarefaService {

    private final TarefaRepository repository;

    public TarefaService(TarefaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Tarefa> listar() {
        return repository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Tarefa buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa", id));
    }

    @Transactional
    public Tarefa criar(String titulo, String descricao) {
        return repository.save(new Tarefa(titulo, descricao));
    }

    @Transactional
    public Tarefa editar(Long id, String titulo, String descricao) {
        Tarefa tarefa = buscar(id);
        tarefa.editar(titulo, descricao);
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
}
