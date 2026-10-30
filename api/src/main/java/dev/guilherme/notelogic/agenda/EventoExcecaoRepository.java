package dev.guilherme.notelogic.agenda;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EventoExcecaoRepository extends JpaRepository<EventoExcecao, Long> {

    /** As que importam para [de, ate]: o dia original cai no período, ou a ocorrência foi movida para dentro dele. */
    @Query("""
            select x from EventoExcecao x
            where x.eventoId in :eventoIds
              and ((x.data between :de and :ate) or (x.novaData between :de and :ate))
            """)
    List<EventoExcecao> doPeriodo(Collection<Long> eventoIds, LocalDate de, LocalDate ate);

    Optional<EventoExcecao> findByEventoIdAndData(Long eventoId, LocalDate data);

    @Modifying
    @Query("delete from EventoExcecao x where x.eventoId = :eventoId and x.data >= :aPartirDe")
    void apagarAPartirDe(Long eventoId, LocalDate aPartirDe);

    @Modifying
    @Query("delete from EventoExcecao x where x.eventoId = :eventoId")
    void apagarDoEvento(Long eventoId);
}
