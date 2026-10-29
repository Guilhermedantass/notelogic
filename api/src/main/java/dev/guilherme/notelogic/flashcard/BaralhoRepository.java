package dev.guilherme.notelogic.flashcard;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BaralhoRepository extends JpaRepository<Baralho, Long> {

    @EntityGraph(attributePaths = "materia")
    @Query("""
            select b from Baralho b
            where b.usuarioId = :usuarioId and (:materiaId is null or b.materia.id = :materiaId)
            order by lower(b.nome)
            """)
    List<Baralho> filtrar(Long usuarioId, Long materiaId);

    @EntityGraph(attributePaths = "materia")
    Optional<Baralho> findByIdAndUsuarioId(Long id, Long usuarioId);
}
