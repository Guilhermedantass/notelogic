package dev.guilherme.notelogic.estatistica;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/estatisticas")
public class EstatisticaController {

    private final EstatisticaService service;

    public EstatisticaController(EstatisticaService service) {
        this.service = service;
    }

    /** As anotações de validação direto no parâmetro: o Spring MVC valida e responde 400 sozinho. */
    @GetMapping
    public Estatisticas calcular(@RequestParam(defaultValue = "30") @Min(1) @Max(366) int dias) {
        return service.calcular(dias);
    }
}
