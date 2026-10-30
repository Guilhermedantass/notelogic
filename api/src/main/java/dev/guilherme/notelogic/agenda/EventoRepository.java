package dev.guilherme.notelogic.agenda;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    /**
     * Os eventos que tocam o período [de, ate]: começam até o último dia e terminam a partir do primeiro.
     * Assim uma semana de provas que começou ontem também aparece. Dia inteiro vem antes dos com horário.
     */
    @Query("""
            select e from Evento e
            left join fetch e.categoria
            left join fetch e.materia
            where e.usuarioId = :usuarioId and e.dataInicio <= :ate and e.dataFim >= :de
            order by e.dataInicio, e.diaInteiro desc, e.horaInicio, e.id
            """)
    List<Evento> noPeriodo(Long usuarioId, LocalDate de, LocalDate ate);

    /** Categoria e matéria vêm no mesmo SELECT: a resposta é montada fora da transação (open-in-view=false). */
    @EntityGraph(attributePaths = { "categoria", "materia" })
    Optional<Evento> findByIdAndUsuarioId(Long id, Long usuarioId);
}
