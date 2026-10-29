package dev.guilherme.notelogic.materia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MateriaRepository extends JpaRepository<Materia, Long> {

    List<Materia> findAllByOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
