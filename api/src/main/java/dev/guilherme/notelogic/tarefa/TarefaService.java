package dev.guilherme.notelogic.tarefa;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Optional<Tarefa> buscar(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Tarefa criar(String titulo, String descricao) {
        return repository.save(new Tarefa(titulo, descricao));
    }

    @Transactional
    public Optional<Tarefa> concluir(Long id) {
        return repository.findById(id).map(tarefa -> {
            tarefa.concluir(); // sem save: o Hibernate percebe a mudança e faz o UPDATE no fim da transação
            return tarefa;
        });
    }

    @Transactional
    public boolean remover(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}
