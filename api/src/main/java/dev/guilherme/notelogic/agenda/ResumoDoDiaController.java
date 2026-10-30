package dev.guilherme.notelogic.agenda;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoDoDiaController {

    private final ResumoDoDiaService service;

    public ResumoDoDiaController(ResumoDoDiaService service) {
        this.service = service;
    }

    /** A parte da agenda na tela Hoje. "Hoje" é o do fuso do app (notelogic.fuso), como o resto da API. */
    @GetMapping("/api/agenda/hoje")
    public ResumoDoDia hoje() {
        return service.resumo();
    }
}
