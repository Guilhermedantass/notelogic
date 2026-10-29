package dev.guilherme.notelogic.tarefa;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class TarefaService {

    private final Map<Long, Tarefa> tarefas = new ConcurrentHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);

    public List<Tarefa> listar() {
        return tarefas.values().stream()
                .sorted(Comparator.comparing(Tarefa::id))
                .toList();
    }

    public Optional<Tarefa> buscar(Long id) {
        return Optional.ofNullable(tarefas.get(id));
    }

    public Tarefa criar(String titulo, String descricao) {
        Long id = proximoId.getAndIncrement();
        Tarefa tarefa = new Tarefa(id, titulo, descricao, false, Instant.now());
        tarefas.put(id, tarefa);
        return tarefa;
    }

    public Optional<Tarefa> concluir(Long id) {
        return Optional.ofNullable(tarefas.computeIfPresent(id, (chave, tarefa) -> tarefa.concluir()));
    }

    public boolean remover(Long id) {
        return tarefas.remove(id) != null;
    }
}
