package dev.guilherme.notelogic.agenda;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaEventoRepository extends JpaRepository<CategoriaEvento, Long> {

    List<CategoriaEvento> findAllByUsuarioIdOrderByNomeAsc(Long usuarioId);

    Optional<CategoriaEvento> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndNomeIgnoreCase(Long usuarioId, String nome);

    boolean existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(Long usuarioId, String nome, Long id);
}
