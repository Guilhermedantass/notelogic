package dev.guilherme.notelogic.materia;

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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/materias")
public class MateriaController {

    private final MateriaService service;

    public MateriaController(MateriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<MateriaResponse> listar() {
        return service.listar().stream().map(MateriaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public MateriaResponse buscar(@PathVariable Long id) {
        return MateriaResponse.de(service.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MateriaResponse criar(@Valid @RequestBody MateriaRequest request) {
        return MateriaResponse.de(service.criar(request.nome(), request.cor()));
    }

    @PutMapping("/{id}")
    public MateriaResponse editar(@PathVariable Long id, @Valid @RequestBody MateriaRequest request) {
        return MateriaResponse.de(service.editar(id, request.nome(), request.cor()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
