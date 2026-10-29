package dev.guilherme.notelogic.estatistica;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SequenciaTest {

    static final LocalDate HOJE = LocalDate.of(2026, 11, 2);

    @Test
    void contaDiasSeguidosAteHoje() {
        Set<LocalDate> dias = Set.of(HOJE, HOJE.minusDays(1), HOJE.minusDays(2), HOJE.minusDays(4));
        assertThat(EstatisticaService.contarSequencia(dias, HOJE)).isEqualTo(3);
    }

    @Test
    void hojeSemEstudoAindaMantemASequenciaDeOntem() {
        Set<LocalDate> dias = Set.of(HOJE.minusDays(1), HOJE.minusDays(2));
        assertThat(EstatisticaService.contarSequencia(dias, HOJE)).isEqualTo(2);
    }

    @Test
    void buracoDeDoisDiasZera() {
        Set<LocalDate> dias = Set.of(HOJE.minusDays(2), HOJE.minusDays(3));
        assertThat(EstatisticaService.contarSequencia(dias, HOJE)).isZero();
    }
}
