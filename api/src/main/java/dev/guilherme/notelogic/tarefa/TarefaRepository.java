package dev.guilherme.notelogic.tarefa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    /**
     * Pendentes primeiro, depois por prazo (sem prazo no fim). Os filtros nulos são ignorados.
     * O @EntityGraph traz a matéria e a recorrência no mesmo SELECT (join), evitando consultas extras por tarefa.
     * Ocorrências expiradas nunca aparecem: só contam no histórico.
     */
    @EntityGraph(attributePaths = { "materia", "recorrencia" })
    @Query("""
            select t from Tarefa t
            where t.usuarioId = :usuarioId
              and t.expirada = false
              and (:materiaId is null or t.materia.id = :materiaId)
              and (:concluida is null or t.concluida = :concluida)
            order by t.concluida, t.prazo asc nulls last, t.id
            """)
    List<Tarefa> filtrar(Long usuarioId, Long materiaId, Boolean concluida);

    @EntityGraph(attributePaths = { "materia", "recorrencia" })
    Optional<Tarefa> findByIdAndUsuarioId(Long id, Long usuarioId);

    /** Para "próximas datas importantes" no Hoje: pendentes de uma prioridade, com prazo no período. */
    @EntityGraph(attributePaths = { "materia" })
    @Query("""
            select t from Tarefa t
            where t.usuarioId = :usuarioId and t.concluida = false and t.expirada = false
              and t.prioridade = :prioridade and t.prazo between :de and :ate
            order by t.prazo, t.id
            """)
    List<Tarefa> pendentesComPrazoEntre(Long usuarioId, Prioridade prioridade, LocalDate de, LocalDate ate);

    // ---- usados pelo GeradorDeOcorrencias e pelo RecorrenciaService ----

    boolean existsByRecorrenciaIdAndDataOcorrencia(Long recorrenciaId, LocalDate data);

    long countByRecorrenciaIdAndConcluidaFalseAndExpiradaFalse(Long recorrenciaId);

    /**
     * @Modifying: um UPDATE em massa, direto no banco, sem carregar as entidades.
     * flushAutomatically grava antes as mudanças pendentes da transação, para o UPDATE enxergar tudo.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Tarefa t set t.expirada = true
            where t.recorrencia.id = :recorrenciaId and t.concluida = false and t.expirada = false
              and t.dataOcorrencia < :hoje
            """)
    int expirarPendentesAntesDe(Long recorrenciaId, LocalDate hoje);

    /** Para "esta e as próximas": tira as ocorrências pendentes a partir do dia (as feitas ficam no histórico). */
    @Modifying(flushAutomatically = true)
    @Query("""
            delete from Tarefa t
            where t.recorrencia.id = :recorrenciaId and t.concluida = false and t.dataOcorrencia >= :data
            """)
    int apagarPendentesAPartirDe(Long recorrenciaId, LocalDate data);

    interface FeitasPorRecorrencia {
        Long getRecorrenciaId();

        long getFeitas();
    }

    @Query("""
            select t.recorrencia.id as recorrenciaId, count(t) as feitas
            from Tarefa t
            where t.usuarioId = :usuarioId and t.concluida = true and t.recorrencia is not null
            group by t.recorrencia.id
            """)
    List<FeitasPorRecorrencia> contarFeitasPorRecorrencia(Long usuarioId);
}
