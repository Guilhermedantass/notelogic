package dev.guilherme.notelogic.estatistica;

import java.time.LocalDate;
import java.util.List;

import dev.guilherme.notelogic.materia.MateriaResponse;

/**
 * Tudo que o painel precisa, numa resposta só.
 *
 * @param taxaDeAcerto fração de revisões BOM ou FÁCIL no período (0 a 1), ou null se não houve revisão
 * @param sequencia    dias seguidos com estudo, terminando hoje (ou ontem, se hoje ainda não teve)
 */
public record Estatisticas(Hoje hoje, int sequencia, Double taxaDeAcerto, List<Dia> dias,
        List<FocoPorMateria> focoPorMateria) {

    public record Hoje(long minutosFocados, long cartoesRevisados, long tarefasConcluidas, long cartoesParaRevisar) {
    }

    /** Um quadradinho do heatmap. Todos os dias do período aparecem, inclusive os zerados. */
    public record Dia(LocalDate data, long minutosFocados, long cartoesRevisados, long tarefasConcluidas) {
    }

    public record FocoPorMateria(MateriaResponse materia, long minutos) {
    }
}
