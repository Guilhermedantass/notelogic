package dev.guilherme.notelogic;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Um Clock que o teste controla: começa num instante fixo e só anda quando mandam. */
public class RelogioAjustavel extends Clock {

    private Instant agora;
    private final ZoneId zona;

    public RelogioAjustavel(Instant agora, ZoneId zona) {
        this.agora = agora;
        this.zona = zona;
    }

    public void ajustar(Instant instante) {
        this.agora = instante;
    }

    public void avancar(Duration duracao) {
        this.agora = agora.plus(duracao);
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new RelogioAjustavel(agora, zone);
    }

    /** Importe nos testes que precisam controlar o tempo. @Primary faz este Clock vencer o de produção. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {

        @Bean
        @Primary
        RelogioAjustavel relogioAjustavel() {
            return new RelogioAjustavel(Instant.parse("2026-11-02T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }
}
