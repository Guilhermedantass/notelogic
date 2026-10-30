package dev.guilherme.notelogic.agenda;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.guilherme.notelogic.agenda.AgendaDtos.CategoriaRequest;
import dev.guilherme.notelogic.agenda.AgendaDtos.CategoriaResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agenda/categorias")
public class CategoriaEventoController {

    private final CategoriaEventoService service;

    public CategoriaEventoController(CategoriaEventoService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return service.listar().stream().map(CategoriaResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse criar(@Valid @RequestBody CategoriaRequest request) {
        return CategoriaResponse.de(service.criar(request));
    }

    @PutMapping("/{id}")
    public CategoriaResponse editar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return CategoriaResponse.de(service.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
