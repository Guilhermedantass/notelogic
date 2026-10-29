package dev.guilherme.notelogic.flashcard;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.guilherme.notelogic.flashcard.FlashcardDtos.CartaoRequest;
import dev.guilherme.notelogic.flashcard.FlashcardDtos.CartaoResponse;
import dev.guilherme.notelogic.flashcard.FlashcardDtos.RevisaoRequest;
import jakarta.validation.Valid;

/** Cartões existem dentro de um baralho (para criar e listar) e por conta própria (para editar e revisar). */
@RestController
public class CartaoController {

    private final CartaoService service;

    public CartaoController(CartaoService service) {
        this.service = service;
    }

    @GetMapping("/api/baralhos/{baralhoId}/cartoes")
    public List<CartaoResponse> listar(@PathVariable Long baralhoId) {
        return service.listar(baralhoId).stream().map(CartaoResponse::de).toList();
    }

    @GetMapping("/api/baralhos/{baralhoId}/estudo")
    public List<CartaoResponse> filaDeEstudo(@PathVariable Long baralhoId) {
        return service.filaDeEstudo(baralhoId).stream().map(CartaoResponse::de).toList();
    }

    @PostMapping("/api/baralhos/{baralhoId}/cartoes")
    @ResponseStatus(HttpStatus.CREATED)
    public CartaoResponse criar(@PathVariable Long baralhoId, @Valid @RequestBody CartaoRequest request) {
        return CartaoResponse.de(service.criar(baralhoId, request.frente(), request.verso()));
    }

    @PutMapping("/api/cartoes/{id}")
    public CartaoResponse editar(@PathVariable Long id, @Valid @RequestBody CartaoRequest request) {
        return CartaoResponse.de(service.editar(id, request.frente(), request.verso()));
    }

    @DeleteMapping("/api/cartoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }

    @PostMapping("/api/cartoes/{id}/revisao")
    public CartaoResponse revisar(@PathVariable Long id, @Valid @RequestBody RevisaoRequest request) {
        return CartaoResponse.de(service.revisar(id, request.resposta()));
    }
}
