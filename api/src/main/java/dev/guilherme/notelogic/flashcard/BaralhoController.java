package dev.guilherme.notelogic.flashcard;

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

import dev.guilherme.notelogic.flashcard.FlashcardDtos.BaralhoRequest;
import dev.guilherme.notelogic.flashcard.FlashcardDtos.BaralhoResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/baralhos")
public class BaralhoController {

    private final BaralhoService service;

    public BaralhoController(BaralhoService service) {
        this.service = service;
    }

    @GetMapping
    public List<BaralhoResponse> listar(@RequestParam(required = false) Long materiaId) {
        return service.listar(materiaId).stream().map(BaralhoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public BaralhoResponse buscar(@PathVariable Long id) {
        return BaralhoResponse.de(service.detalhar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaralhoResponse criar(@Valid @RequestBody BaralhoRequest request) {
        Baralho baralho = service.criar(request.nome(), request.descricao(), request.materiaId());
        return BaralhoResponse.de(service.detalhar(baralho.getId()));
    }

    @PutMapping("/{id}")
    public BaralhoResponse editar(@PathVariable Long id, @Valid @RequestBody BaralhoRequest request) {
        service.editar(id, request.nome(), request.descricao(), request.materiaId());
        return BaralhoResponse.de(service.detalhar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
