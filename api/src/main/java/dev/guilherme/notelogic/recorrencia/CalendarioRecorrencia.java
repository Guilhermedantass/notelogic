package dev.guilherme.notelogic.recorrencia;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Contas de calendário de uma RegraRecorrencia. Lógica pura (como o AgendadorSm2): sem Spring,
 * sem banco, sem relógio. Toda data entra por parâmetro, então os testes cobrem qualquer dia do ano.
 */
public final class CalendarioRecorrencia {

    /** Nenhuma regra fica mais de um ano sem ocorrer (a maior é "a cada 365 dias"). */
    private static final int LIMITE_BUSCA_DIAS = 400;
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private CalendarioRecorrencia() {
    }

    /** O dia cai na regra? (sem olhar o limite de ocorrências, que depende de contar desde o início) */
    static boolean caiNoDia(RegraRecorrencia regra, LocalDate dia) {
        if (dia.isBefore(regra.inicio()) || (regra.fim() != null && dia.isAfter(regra.fim()))) {
            return false;
        }
        return switch (regra.tipo()) {
            case DIARIA -> true;
            case DIAS_UTEIS -> dia.getDayOfWeek() != DayOfWeek.SATURDAY && dia.getDayOfWeek() != DayOfWeek.SUNDAY;
            case SEMANAL -> regra.diasSemana().contains(dia.getDayOfWeek());
            case A_CADA_N_DIAS -> ChronoUnit.DAYS.between(regra.inicio(), dia) % regra.intervaloDias() == 0;
            // "dia 31" num mês de 30 dias cai no dia 30; em fevereiro, no 28 (ou 29)
            case MENSAL -> dia.getDayOfMonth() == Math.min(regra.diaDoMes(), dia.lengthOfMonth());
        };
    }

    /** A primeira data da regra DEPOIS de {@code depoisDe}, se ainda houver alguma. */
    public static Optional<LocalDate> proxima(RegraRecorrencia regra, LocalDate depoisDe) {
        LocalDate dia = depoisDe.plusDays(1);
        if (dia.isBefore(regra.inicio())) {
            dia = regra.inicio();
        }
        for (int i = 0; i < LIMITE_BUSCA_DIAS; i++, dia = dia.plusDays(1)) {
            if (regra.fim() != null && dia.isAfter(regra.fim())) {
                return Optional.empty();
            }
            if (caiNoDia(regra, dia)) {
                boolean passouDoLimite = regra.maxOcorrencias() != null
                        && entre(regra, regra.inicio(), dia).size() > regra.maxOcorrencias();
                return passouDoLimite ? Optional.empty() : Optional.of(dia);
            }
        }
        return Optional.empty();
    }

    /** Todas as datas da regra entre {@code de} e {@code ate}, inclusive (sem o limite de ocorrências). */
    public static List<LocalDate> entre(RegraRecorrencia regra, LocalDate de, LocalDate ate) {
        List<LocalDate> datas = new ArrayList<>();
        for (LocalDate dia = de; !dia.isAfter(ate); dia = dia.plusDays(1)) {
            if (caiNoDia(regra, dia)) {
                datas.add(dia);
            }
        }
        return datas;
    }

    /**
     * As datas da regra entre {@code de} e {@code ate}, inclusive, JÁ respeitando o limite de ocorrências
     * ("depois de 10 vezes" conta desde o início, mesmo que o período pedido comece depois).
     */
    public static List<LocalDate> ocorrencias(RegraRecorrencia regra, LocalDate de, LocalDate ate) {
        if (regra.maxOcorrencias() == null) {
            return entre(regra, de.isBefore(regra.inicio()) ? regra.inicio() : de, ate);
        }
        List<LocalDate> todas = entre(regra, regra.inicio(), ate);
        return todas.subList(0, Math.min(todas.size(), regra.maxOcorrencias())).stream()
                .filter(dia -> !dia.isBefore(de))
                .toList();
    }

    /** "Todo dia", "Dias úteis", "Toda segunda e quarta", "A cada 3 dias", "Todo mês no dia 15". */
    public static String descrever(RegraRecorrencia regra) {
        return switch (regra.tipo()) {
            case DIARIA -> "Todo dia";
            case DIAS_UTEIS -> "Dias úteis";
            // "Toda segunda", mas "Todo sábado": sábado e domingo são masculinos
            case SEMANAL -> (soFimDeSemana(regra) ? "Todo " : "Toda ") + juntar(regra.diasSemana().stream()
                    .sorted(Comparator.naturalOrder())
                    .map(d -> d.getDisplayName(TextStyle.FULL, PT_BR).replace("-feira", ""))
                    .toList());
            case A_CADA_N_DIAS -> regra.intervaloDias() == 1 ? "Todo dia" : "A cada " + regra.intervaloDias() + " dias";
            case MENSAL -> "Todo mês no dia " + regra.diaDoMes();
        };
    }

    private static boolean soFimDeSemana(RegraRecorrencia regra) {
        return regra.diasSemana().stream().allMatch(d -> d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY);
    }

    /** ["segunda", "quarta", "sexta"] → "segunda, quarta e sexta" */
    private static String juntar(List<String> partes) {
        if (partes.size() <= 1) {
            return String.join("", partes);
        }
        return String.join(", ", partes.subList(0, partes.size() - 1)) + " e " + partes.getLast();
    }
}
