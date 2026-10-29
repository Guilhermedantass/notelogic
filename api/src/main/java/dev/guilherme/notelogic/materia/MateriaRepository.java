package dev.guilherme.notelogic.materia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Toda consulta recebe o usuarioId: não existe caminho para ler a matéria de outra pessoa. */
public interface MateriaRepository extends JpaRepository<Materia, Long> {

    List<Materia> findAllByUsuarioIdOrderByNomeAsc(Long usuarioId);

    Optional<Materia> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndNomeIgnoreCase(Long usuarioId, String nome);

    boolean existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(Long usuarioId, String nome, Long id);
}
