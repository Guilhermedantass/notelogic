package dev.guilherme.notelogic.recorrencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface RecorrenciaRepository extends JpaRepository<Recorrencia, Long> {

    /**
     * Trava pessimista (SELECT ... FOR UPDATE): enquanto esta transação gera as ocorrências, outra requisição
     * do mesmo usuário que tente o mesmo ESPERA. Sem isso, duas abas abertas criariam a tarefa do dia duas vezes.
     * (Sem join fetch de propósito: o Postgres não aceita FOR UPDATE no lado opcional de um left join.)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Recorrencia r where r.usuarioId = :usuarioId order by r.id")
    List<Recorrencia> travarDoUsuario(Long usuarioId);

    @EntityGraph(attributePaths = "materia")
    List<Recorrencia> findByUsuarioIdOrderByIdAsc(Long usuarioId);

    @EntityGraph(attributePaths = "materia")
    Optional<Recorrencia> findByIdAndUsuarioId(Long id, Long usuarioId);
}
