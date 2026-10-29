package dev.guilherme.notelogic.estatistica;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Consultas de leitura em SQL puro, atravessando as tabelas de vários módulos.
 *
 * Por que não JPA aqui: o resultado não é uma entidade, é uma agregação ("minutos por dia no fuso de São Paulo").
 * O JPQL não tem "at time zone" do Postgres, e carregar milhares de entidades para somar em Java seria um desperdício.
 * O JdbcClient roda o SQL e mapeia cada linha para um record.
 */
@Repository
public class EstatisticaConsultas {

    public record PorDia(LocalDate dia, long valor) {
    }

    public record RevisoesNoDia(LocalDate dia, long total, long acertos) {
    }

    public record MinutosPorMateria(Long materiaId, String nome, String cor, long segundos) {
    }

    /** O dia (no fuso do app) em que cada coisa aconteceu. */
    private static final String DIA = "cast((%s at time zone :fuso) as date)";

    private final JdbcClient jdbc;

    public EstatisticaConsultas(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Segundos de FOCO por dia. Sessões ainda em andamento não contam. */
    public Map<LocalDate, Long> segundosDeFocoPorDia(Instant inicio, Instant fim, ZoneId fuso) {
        String sql = """
                select %s as dia, sum(segundos_focados) as valor
                from sessao_foco
                where tipo = 'FOCO' and status <> 'EM_ANDAMENTO'
                  and iniciada_em >= :inicio and iniciada_em < :fim
                group by 1
                """.formatted(DIA.formatted("iniciada_em"));
        return porDia(sql, inicio, fim, fuso);
    }

    public Map<LocalDate, Long> tarefasConcluidasPorDia(Instant inicio, Instant fim, ZoneId fuso) {
        String sql = """
                select %s as dia, count(*) as valor
                from tarefa
                where concluida and concluida_em >= :inicio and concluida_em < :fim
                group by 1
                """.formatted(DIA.formatted("concluida_em"));
        return porDia(sql, inicio, fim, fuso);
    }

    public Map<LocalDate, RevisoesNoDia> revisoesPorDia(Instant inicio, Instant fim, ZoneId fuso) {
        String sql = """
                select %s as dia,
                       count(*) as total,
                       count(*) filter (where resposta in ('BOM', 'FACIL')) as acertos
                from revisao
                where respondida_em >= :inicio and respondida_em < :fim
                group by 1
                """.formatted(DIA.formatted("respondida_em"));
        return jdbc.sql(sql)
                .param("fuso", fuso.getId())
                .param("inicio", utc(inicio))
                .param("fim", utc(fim))
                .query((rs, n) -> new RevisoesNoDia(rs.getObject("dia", LocalDate.class), rs.getLong("total"),
                        rs.getLong("acertos")))
                .list().stream()
                .collect(Collectors.toMap(RevisoesNoDia::dia, r -> r));
    }

    public List<MinutosPorMateria> segundosDeFocoPorMateria(Instant inicio, Instant fim) {
        return jdbc.sql("""
                select m.id, m.nome, m.cor, sum(s.segundos_focados) as segundos
                from sessao_foco s
                join materia m on m.id = s.materia_id
                where s.tipo = 'FOCO' and s.status <> 'EM_ANDAMENTO'
                  and s.iniciada_em >= :inicio and s.iniciada_em < :fim
                group by m.id, m.nome, m.cor
                order by segundos desc
                """)
                .param("inicio", utc(inicio))
                .param("fim", utc(fim))
                .query((rs, n) -> new MinutosPorMateria(rs.getLong("id"), rs.getString("nome"), rs.getString("cor"),
                        rs.getLong("segundos")))
                .list();
    }

    /** Dias com qualquer atividade de estudo (foco ou revisão), do mais recente para o mais antigo. */
    public List<LocalDate> diasComEstudo(Instant desde, ZoneId fuso) {
        return jdbc.sql("""
                select %s as dia from sessao_foco
                where tipo = 'FOCO' and status <> 'EM_ANDAMENTO' and iniciada_em >= :desde
                union
                select %s as dia from revisao where respondida_em >= :desde
                order by dia desc
                """.formatted(DIA.formatted("iniciada_em"), DIA.formatted("respondida_em")))
                .param("fuso", fuso.getId())
                .param("desde", utc(desde))
                .query(LocalDate.class)
                .list();
    }

    public long cartoesParaRevisar(LocalDate hoje) {
        return jdbc.sql("select count(*) from cartao where proxima_revisao <= :hoje")
                .param("hoje", hoje)
                .query(Long.class)
                .single();
    }

    private Map<LocalDate, Long> porDia(String sql, Instant inicio, Instant fim, ZoneId fuso) {
        return jdbc.sql(sql)
                .param("fuso", fuso.getId())
                .param("inicio", utc(inicio))
                .param("fim", utc(fim))
                .query((rs, n) -> new PorDia(rs.getObject("dia", LocalDate.class), rs.getLong("valor")))
                .list().stream()
                .collect(Collectors.toMap(PorDia::dia, PorDia::valor));
    }

    /** O driver do Postgres mapeia OffsetDateTime direto para timestamp with time zone. */
    private static OffsetDateTime utc(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }
}
