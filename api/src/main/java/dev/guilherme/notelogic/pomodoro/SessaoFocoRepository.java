package dev.guilherme.notelogic.pomodoro;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoFocoRepository extends JpaRepository<SessaoFoco, Long> {

    boolean existsByStatus(StatusSessao status);

    @EntityGraph(attributePaths = { "tarefa", "materia" })
    Optional<SessaoFoco> findFirstByStatus(StatusSessao status);

    /** Intervalo meio-aberto [inicio, fim): o padrão mais seguro para períodos de tempo. */
    @EntityGraph(attributePaths = { "tarefa", "materia" })
    List<SessaoFoco> findByIniciadaEmGreaterThanEqualAndIniciadaEmLessThanOrderByIniciadaEmDesc(Instant inicio,
            Instant fim);

    @Override
    @EntityGraph(attributePaths = { "tarefa", "materia" })
    Optional<SessaoFoco> findById(Long id);
}
