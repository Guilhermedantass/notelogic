package dev.guilherme.notelogic.agenda;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    /**
     * Os eventos que NÃO se repetem e tocam o período [de, ate]: começam até o último dia e terminam a partir do
     * primeiro. Assim uma semana de provas que começou ontem também aparece.
     */
    @Query("""
            select e from Evento e
            left join fetch e.categoria
            left join fetch e.materia
            where e.usuarioId = :usuarioId and e.repeticao.tipo is null
              and e.dataInicio <= :ate and e.dataFim >= :de
            """)
    List<Evento> avulsosNoPeriodo(Long usuarioId, LocalDate de, LocalDate ate);

    /**
     * Os que se repetem e podem ter ocorrência até {@code ate}. Não filtra pelo fim da regra: uma ocorrência
     * de antes pode ter sido movida para dentro do período (as datas saem do CalendarioRecorrencia).
     */
    @Query("""
            select e from Evento e
            left join fetch e.categoria
            left join fetch e.materia
            where e.usuarioId = :usuarioId and e.repeticao.tipo is not null and e.dataInicio <= :ate
            """)
    List<Evento> recorrentesAte(Long usuarioId, LocalDate ate);

    /** Categoria e matéria vêm no mesmo SELECT: a resposta é montada fora da transação (open-in-view=false). */
    @EntityGraph(attributePaths = { "categoria", "materia" })
    Optional<Evento> findByIdAndUsuarioId(Long id, Long usuarioId);
}
