package dev.guilherme.notelogic.estatistica;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.estatistica.EstatisticaConsultas.RevisoesNoDia;
import dev.guilherme.notelogic.estatistica.Estatisticas.Dia;
import dev.guilherme.notelogic.estatistica.Estatisticas.FocoPorMateria;
import dev.guilherme.notelogic.estatistica.Estatisticas.Hoje;
import dev.guilherme.notelogic.materia.MateriaResponse;
import dev.guilherme.notelogic.usuario.UsuarioAtual;

@Service
public class EstatisticaService {

    /** Até onde olhar para trás ao contar a sequência de dias. */
    private static final int LIMITE_SEQUENCIA_DIAS = 366;

    private final EstatisticaConsultas consultas;
    private final Clock clock;
    private final UsuarioAtual usuario;

    public EstatisticaService(EstatisticaConsultas consultas, Clock clock, UsuarioAtual usuario) {
        this.consultas = consultas;
        this.clock = clock;
        this.usuario = usuario;
    }

    /** Os últimos {@code dias} dias, terminando hoje. Uma transação só: todos os números vêm do mesmo "retrato". */
    @Transactional(readOnly = true)
    public Estatisticas calcular(int dias) {
        Long usuarioId = usuario.id();
        ZoneId fuso = clock.getZone();
        LocalDate hoje = LocalDate.now(clock);
        LocalDate primeiroDia = hoje.minusDays(dias - 1L);
        Instant inicio = primeiroDia.atStartOfDay(fuso).toInstant();
        Instant fim = hoje.plusDays(1).atStartOfDay(fuso).toInstant();

        Map<LocalDate, Long> foco = consultas.segundosDeFocoPorDia(usuarioId, inicio, fim, fuso);
        Map<LocalDate, Long> tarefas = consultas.tarefasConcluidasPorDia(usuarioId, inicio, fim, fuso);
        Map<LocalDate, RevisoesNoDia> revisoes = consultas.revisoesPorDia(usuarioId, inicio, fim, fuso);

        List<Dia> serie = new ArrayList<>();
        for (LocalDate d = primeiroDia; !d.isAfter(hoje); d = d.plusDays(1)) {
            RevisoesNoDia r = revisoes.get(d);
            serie.add(new Dia(d, foco.getOrDefault(d, 0L) / 60, r != null ? r.total() : 0,
                    tarefas.getOrDefault(d, 0L)));
        }

        Dia deHoje = serie.getLast();
        Hoje resumoHoje = new Hoje(deHoje.minutosFocados(), deHoje.cartoesRevisados(), deHoje.tarefasConcluidas(),
                consultas.cartoesParaRevisar(usuarioId, hoje));

        List<FocoPorMateria> porMateria = consultas.segundosDeFocoPorMateria(usuarioId, inicio, fim).stream()
                .map(m -> new FocoPorMateria(new MateriaResponse(m.materiaId(), m.nome(), m.cor()),
                        m.segundos() / 60))
                .toList();

        return new Estatisticas(resumoHoje, sequencia(usuarioId, hoje, fuso), taxaDeAcerto(revisoes), serie, porMateria);
    }

    private int sequencia(Long usuarioId, LocalDate hoje, ZoneId fuso) {
        Instant desde = hoje.minusDays(LIMITE_SEQUENCIA_DIAS).atStartOfDay(fuso).toInstant();
        Set<LocalDate> estudados = new HashSet<>(consultas.diasComEstudo(usuarioId, desde, fuso));
        return contarSequencia(estudados, hoje);
    }

    /**
     * Conta para trás a partir de hoje. Se hoje ainda não teve estudo, começa de ontem:
     * às 8h da manhã a sequência de ontem ainda está "viva".
     */
    static int contarSequencia(Set<LocalDate> estudados, LocalDate hoje) {
        LocalDate dia = estudados.contains(hoje) ? hoje : hoje.minusDays(1);
        int sequencia = 0;
        while (estudados.contains(dia)) {
            sequencia++;
            dia = dia.minusDays(1);
        }
        return sequencia;
    }

    private static Double taxaDeAcerto(Map<LocalDate, RevisoesNoDia> revisoes) {
        long total = revisoes.values().stream().mapToLong(RevisoesNoDia::total).sum();
        long acertos = revisoes.values().stream().mapToLong(RevisoesNoDia::acertos).sum();
        return total == 0 ? null : (double) acertos / total;
    }
}
