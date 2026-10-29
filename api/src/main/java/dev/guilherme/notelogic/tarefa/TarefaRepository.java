package dev.guilherme.notelogic.tarefa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    /**
     * Pendentes primeiro, depois por prazo (sem prazo no fim). Os filtros nulos são ignorados.
     * O @EntityGraph traz a matéria no mesmo SELECT (join), evitando uma consulta extra por tarefa.
     */
    @EntityGraph(attributePaths = "materia")
    @Query("""
            select t from Tarefa t
            where (:materiaId is null or t.materia.id = :materiaId)
              and (:concluida is null or t.concluida = :concluida)
            order by t.concluida, t.prazo asc nulls last, t.id
            """)
    List<Tarefa> filtrar(Long materiaId, Boolean concluida);

    @Override
    @EntityGraph(attributePaths = "materia")
    Optional<Tarefa> findById(Long id);
}
