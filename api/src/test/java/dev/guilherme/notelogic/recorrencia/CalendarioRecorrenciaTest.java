package dev.guilherme.notelogic.recorrencia;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.guilherme.notelogic.erro.RegraVioladaException;

/** Teste unitário puro: sem Spring, sem banco. As datas são escolhidas a dedo para pegar os casos de borda. */
class CalendarioRecorrenciaTest {

    /** 2 de novembro de 2026 é uma segunda-feira. */
    static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 2);

    static RegraRecorrencia regra(TipoRecorrencia tipo) {
        return new RegraRecorrencia(tipo, null, null, null, SEGUNDA, null, null);
    }

    @Test
    void diariaCaiTodoDia() {
        assertThat(CalendarioRecorrencia.proxima(regra(TipoRecorrencia.DIARIA), SEGUNDA)).hasValue(SEGUNDA.plusDays(1));
    }

    @Test
    void diasUteisPulamOFimDeSemana() {
        LocalDate sexta = SEGUNDA.plusDays(4);
        assertThat(CalendarioRecorrencia.proxima(regra(TipoRecorrencia.DIAS_UTEIS), sexta))
                .hasValue(SEGUNDA.plusDays(7)); // a segunda seguinte
    }

    @Test
    void semanalSoNosDiasEscolhidos() {
        var segundaEQuarta = new RegraRecorrencia(TipoRecorrencia.SEMANAL, Set.of(MONDAY, WEDNESDAY), null, null,
                SEGUNDA, null, null);
        assertThat(CalendarioRecorrencia.entre(segundaEQuarta, SEGUNDA, SEGUNDA.plusDays(13)))
                .containsExactly(SEGUNDA, SEGUNDA.plusDays(2), SEGUNDA.plusDays(7), SEGUNDA.plusDays(9));
    }

    @Test
    void aCadaNDiasContaAPartirDoInicio() {
        var aCada3 = new RegraRecorrencia(TipoRecorrencia.A_CADA_N_DIAS, null, 3, null, SEGUNDA, null, null);
        assertThat(CalendarioRecorrencia.entre(aCada3, SEGUNDA.minusDays(5), SEGUNDA.plusDays(7)))
                .containsExactly(SEGUNDA, SEGUNDA.plusDays(3), SEGUNDA.plusDays(6)); // nada antes do início
    }

    @Test
    void mensalNoDia31CaiNoUltimoDiaDosMesesCurtos() {
        var dia31 = new RegraRecorrencia(TipoRecorrencia.MENSAL, null, null, 31, LocalDate.of(2027, 1, 1), null, null);
        assertThat(CalendarioRecorrencia.entre(dia31, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 4, 30)))
                .containsExactly(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31),
                        LocalDate.of(2027, 4, 30));
        // e em ano bissexto, 29 de fevereiro
        assertThat(CalendarioRecorrencia.proxima(dia31, LocalDate.of(2028, 2, 1))).hasValue(LocalDate.of(2028, 2, 29));
    }

    @Test
    void respeitaOFim() {
        var ateQuarta = new RegraRecorrencia(TipoRecorrencia.DIARIA, null, null, null, SEGUNDA, SEGUNDA.plusDays(2), null);
        assertThat(CalendarioRecorrencia.proxima(ateQuarta, SEGUNDA.plusDays(1))).hasValue(SEGUNDA.plusDays(2));
        assertThat(CalendarioRecorrencia.proxima(ateQuarta, SEGUNDA.plusDays(2))).isEmpty();
    }

    @Test
    void respeitaONumeroMaximoDeVezes() {
        var tresVezes = new RegraRecorrencia(TipoRecorrencia.SEMANAL, Set.of(FRIDAY), null, null, SEGUNDA, null, 3);
        LocalDate terceiraSexta = SEGUNDA.plusDays(4 + 14);
        assertThat(CalendarioRecorrencia.proxima(tresVezes, terceiraSexta.minusDays(1))).hasValue(terceiraSexta);
        assertThat(CalendarioRecorrencia.proxima(tresVezes, terceiraSexta)).isEmpty();
    }

    @Test
    void comecaNoInicioMesmoPerguntandoDeAntes() {
        assertThat(CalendarioRecorrencia.proxima(regra(TipoRecorrencia.DIARIA), SEGUNDA.minusDays(30))).hasValue(SEGUNDA);
    }

    @Test
    void descreveEmPortugues() {
        assertThat(CalendarioRecorrencia.descrever(regra(TipoRecorrencia.DIARIA))).isEqualTo("Todo dia");
        assertThat(CalendarioRecorrencia.descrever(regra(TipoRecorrencia.DIAS_UTEIS))).isEqualTo("Dias úteis");
        assertThat(CalendarioRecorrencia.descrever(new RegraRecorrencia(TipoRecorrencia.SEMANAL,
                Set.of(FRIDAY, MONDAY, WEDNESDAY), null, null, SEGUNDA, null, null)))
                .isEqualTo("Toda segunda, quarta e sexta");
        assertThat(CalendarioRecorrencia.descrever(new RegraRecorrencia(TipoRecorrencia.SEMANAL,
                Set.of(SATURDAY), null, null, SEGUNDA, null, null))).isEqualTo("Todo sábado");
        assertThat(CalendarioRecorrencia.descrever(new RegraRecorrencia(TipoRecorrencia.A_CADA_N_DIAS, null, 3,
                null, SEGUNDA, null, null))).isEqualTo("A cada 3 dias");
        assertThat(CalendarioRecorrencia.descrever(new RegraRecorrencia(TipoRecorrencia.MENSAL, null, null, 15,
                SEGUNDA, null, null))).isEqualTo("Todo mês no dia 15");
    }

    @Test
    void validaACoerenciaDaRegra() {
        var semanalSemDias = new RegraRecorrencia(TipoRecorrencia.SEMANAL, Set.of(), null, null, SEGUNDA, null, null);
        assertThatThrownBy(semanalSemDias::validar).isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("dia da semana");

        var fimAntes = new RegraRecorrencia(TipoRecorrencia.DIARIA, null, null, null, SEGUNDA, SEGUNDA.minusDays(1), null);
        assertThatThrownBy(fimAntes::validar).isInstanceOf(RegraVioladaException.class);
    }
}
