package dev.guilherme.notelogic.recorrencia;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.guilherme.notelogic.recorrencia.RecorrenciaDtos.RecorrenciaRequest;
import dev.guilherme.notelogic.recorrencia.RecorrenciaDtos.RecorrenciaResponse;
import jakarta.validation.Valid;

/**
 * As regras de repetição. As ocorrências (tarefas de cada dia) continuam em /api/tarefas:
 * "só esta" é editar/apagar a tarefa; "esta e as próximas" é editar/encerrar a regra aqui, a partir do dia dela.
 */
@RestController
@RequestMapping("/api/recorrencias")
public class RecorrenciaController {

    private final RecorrenciaService service;

    public RecorrenciaController(RecorrenciaService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecorrenciaResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecorrenciaResponse criar(@Valid @RequestBody RecorrenciaRequest request) {
        return respostaDe(service.criar(request).getId());
    }

    @PutMapping("/{id}")
    public RecorrenciaResponse editar(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate aPartirDe,
            @Valid @RequestBody RecorrenciaRequest request) {
        service.editarAPartirDe(id, aPartirDe, request);
        return respostaDe(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void encerrar(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate aPartirDe) {
        service.encerrarAPartirDe(id, aPartirDe);
    }

    private RecorrenciaResponse respostaDe(Long id) {
        return service.listar().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }
}
