package dev.guilherme.notelogic.agenda;

import java.time.LocalDate;
import java.util.List;

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

import dev.guilherme.notelogic.agenda.AgendaDtos.EventoRequest;
import dev.guilherme.notelogic.agenda.AgendaDtos.EventoResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService service;

    public EventoController(EventoService service) {
        this.service = service;
    }

    /** Sempre por período (a agenda pede a semana ou o mês que está na tela): {@code ?de=2026-11-02&ate=2026-11-08}. */
    @GetMapping
    public List<EventoResponse> listar(@RequestParam LocalDate de, @RequestParam LocalDate ate) {
        return service.noPeriodo(de, ate).stream().map(EventoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public EventoResponse buscar(@PathVariable Long id) {
        return EventoResponse.de(service.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponse criar(@Valid @RequestBody EventoRequest request) {
        return EventoResponse.de(service.criar(request));
    }

    @PutMapping("/{id}")
    public EventoResponse editar(@PathVariable Long id, @Valid @RequestBody EventoRequest request) {
        return EventoResponse.de(service.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
