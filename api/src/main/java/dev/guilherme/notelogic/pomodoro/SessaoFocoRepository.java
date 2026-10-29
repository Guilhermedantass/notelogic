package dev.guilherme.notelogic.pomodoro;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoFocoRepository extends JpaRepository<SessaoFoco, Long> {

    boolean existsByUsuarioIdAndStatus(Long usuarioId, StatusSessao status);

    @EntityGraph(attributePaths = { "tarefa", "materia" })
    Optional<SessaoFoco> findFirstByUsuarioIdAndStatus(Long usuarioId, StatusSessao status);

    /** Intervalo meio-aberto [inicio, fim): o padrão mais seguro para períodos de tempo. */
    @EntityGraph(attributePaths = { "tarefa", "materia" })
    List<SessaoFoco> findByUsuarioIdAndIniciadaEmGreaterThanEqualAndIniciadaEmLessThanOrderByIniciadaEmDesc(
            Long usuarioId, Instant inicio, Instant fim);

    @EntityGraph(attributePaths = { "tarefa", "materia" })
    Optional<SessaoFoco> findByIdAndUsuarioId(Long id, Long usuarioId);
}
