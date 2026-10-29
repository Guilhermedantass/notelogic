package dev.guilherme.notelogic.flashcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** Teste unitário puro: sem Spring, sem banco. Roda em milissegundos. */
class AgendadorSm2Test {

    @Test
    void acertosSeguidosSeguemAEscadaDoSm2() {
        EstadoAgendamento e = EstadoAgendamento.novo();

        e = AgendadorSm2.agendar(e, Resposta.BOM);
        assertThat(e.intervaloDias()).isEqualTo(1);

        e = AgendadorSm2.agendar(e, Resposta.BOM);
        assertThat(e.intervaloDias()).isEqualTo(6);

        e = AgendadorSm2.agendar(e, Resposta.BOM);
        assertThat(e.intervaloDias()).isEqualTo(15); // 6 x 2.5
        assertThat(e.repeticoes()).isEqualTo(3);
    }

    @Test
    void bomMantemAFacilidade() {
        EstadoAgendamento e = AgendadorSm2.agendar(EstadoAgendamento.novo(), Resposta.BOM);
        assertThat(e.facilidade()).isCloseTo(2.5, within(1e-9));
    }

    @Test
    void dificilBaixaEFacilSobeAFacilidade() {
        assertThat(AgendadorSm2.agendar(EstadoAgendamento.novo(), Resposta.DIFICIL).facilidade())
                .isCloseTo(2.36, within(1e-9));
        assertThat(AgendadorSm2.agendar(EstadoAgendamento.novo(), Resposta.FACIL).facilidade())
                .isCloseTo(2.6, within(1e-9));
    }

    @Test
    void errarZeraAsRepeticoesMasNaoMexeNaFacilidade() {
        EstadoAgendamento aprendido = new EstadoAgendamento(4, 40, 2.2);

        EstadoAgendamento e = AgendadorSm2.agendar(aprendido, Resposta.ERREI);

        assertThat(e.repeticoes()).isZero();
        assertThat(e.intervaloDias()).isZero(); // volta ainda hoje
        assertThat(e.facilidade()).isEqualTo(2.2);

        // depois de errar, recomeça a escada: 1 dia, 6 dias...
        assertThat(AgendadorSm2.agendar(e, Resposta.BOM).intervaloDias()).isEqualTo(1);
    }

    @Test
    void facilidadeNuncaFicaAbaixoDoMinimo() {
        EstadoAgendamento e = EstadoAgendamento.novo();
        for (int i = 0; i < 20; i++) {
            e = AgendadorSm2.agendar(e, Resposta.DIFICIL);
        }
        assertThat(e.facilidade()).isEqualTo(AgendadorSm2.FACILIDADE_MINIMA);
    }
}
