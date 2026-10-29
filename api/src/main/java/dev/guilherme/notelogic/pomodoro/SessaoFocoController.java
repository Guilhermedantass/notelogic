package dev.guilherme.notelogic.pomodoro;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.guilherme.notelogic.pomodoro.SessaoFocoDtos.IniciarSessaoRequest;
import dev.guilherme.notelogic.pomodoro.SessaoFocoDtos.SessaoResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pomodoro/sessoes")
public class SessaoFocoController {

    private final SessaoFocoService service;

    public SessaoFocoController(SessaoFocoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoResponse iniciar(@Valid @RequestBody IniciarSessaoRequest request) {
        return SessaoResponse.de(
                service.iniciar(request.tipo(), request.duracaoMin(), request.tarefaId(), request.materiaId()));
    }

    /** 200 com a sessão, ou 204 (sem corpo) se não houver nenhuma rodando. Usado pelo front para retomar o timer. */
    @GetMapping("/atual")
    public ResponseEntity<SessaoResponse> atual() {
        return service.atual()
                .map(sessao -> ResponseEntity.ok(SessaoResponse.de(sessao)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}/concluir")
    public SessaoResponse concluir(@PathVariable Long id) {
        return SessaoResponse.de(service.concluir(id));
    }

    @PatchMapping("/{id}/interromper")
    public SessaoResponse interromper(@PathVariable Long id) {
        return SessaoResponse.de(service.interromper(id));
    }

    /** Sem parâmetros, devolve as sessões de hoje. */
    @GetMapping
    public List<SessaoResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        LocalDate hoje = service.hoje();
        return service.listar(de != null ? de : hoje, ate != null ? ate : hoje).stream()
                .map(SessaoResponse::de)
                .toList();
    }
}
