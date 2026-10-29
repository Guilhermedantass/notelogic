package dev.guilherme.notelogic.tarefa;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tarefas")
public class TarefaController {

    private final TarefaService service;

    public TarefaController(TarefaService service) {
        this.service = service;
    }

    @GetMapping
    public List<TarefaResponse> listar(@RequestParam(required = false) Long materiaId,
            @RequestParam(required = false) Boolean concluida) {
        return service.listar(materiaId, concluida).stream().map(TarefaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public TarefaResponse buscar(@PathVariable Long id) {
        return TarefaResponse.de(service.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarefaResponse criar(@Valid @RequestBody TarefaRequest request) {
        return TarefaResponse.de(service.criar(request));
    }

    @PutMapping("/{id}")
    public TarefaResponse editar(@PathVariable Long id, @Valid @RequestBody TarefaRequest request) {
        return TarefaResponse.de(service.editar(id, request));
    }

    @PatchMapping("/{id}/concluir")
    public TarefaResponse concluir(@PathVariable Long id) {
        return TarefaResponse.de(service.concluir(id));
    }

    @PatchMapping("/{id}/reabrir")
    public TarefaResponse reabrir(@PathVariable Long id) {
        return TarefaResponse.de(service.reabrir(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
