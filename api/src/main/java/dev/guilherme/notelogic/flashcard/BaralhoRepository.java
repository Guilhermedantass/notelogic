package dev.guilherme.notelogic.flashcard;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BaralhoRepository extends JpaRepository<Baralho, Long> {

    @EntityGraph(attributePaths = "materia")
    @Query("select b from Baralho b where (:materiaId is null or b.materia.id = :materiaId) order by lower(b.nome)")
    List<Baralho> filtrar(Long materiaId);

    @Override
    @EntityGraph(attributePaths = "materia")
    Optional<Baralho> findById(Long id);
}
