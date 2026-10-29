package dev.guilherme.notelogic.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quem precisa saber "que horas são" recebe um Clock por injeção em vez de chamar LocalDate.now().
 * Assim o fuso fica num lugar só e os testes podem controlar o tempo.
 */
@Configuration
public class Relogio {

    @Bean
    Clock clock(@Value("${notelogic.fuso}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }
}
