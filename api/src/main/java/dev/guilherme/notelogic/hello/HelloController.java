package dev.guilherme.notelogic.hello;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    public record Saudacao(String mensagem, Instant horario){}

    @GetMapping("/hello")
    public Saudacao hello(@RequestParam(defaultValue = "mundo") String nome){
        return new Saudacao("Olá, " + nome + "!", Instant.now());
    }
}