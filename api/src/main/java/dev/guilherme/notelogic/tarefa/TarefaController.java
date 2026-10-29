package dev.guilherme.notelogic.tarefa;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tarefas")
public class TarefaController {

    private final TarefaService service;

    public TarefaController(TarefaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Tarefa> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Tarefa buscar(@PathVariable Long id) {
        return service.buscar(id).orElseThrow(this::naoEncontrada);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tarefa criar(@RequestBody NovaTarefaRequest request) {
        return service.criar(request.titulo(), request.descricao());
    }

    @PatchMapping("/{id}/concluir")
    public Tarefa concluir(@PathVariable Long id) {
        return service.concluir(id).orElseThrow(this::naoEncontrada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        if (!service.remover(id)) {
            throw naoEncontrada();
        }
    }

    private ResponseStatusException naoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada");
    }
}
