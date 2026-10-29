package dev.guilherme.notelogic.flashcard;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CartaoRepository extends JpaRepository<Cartao, Long> {

    List<Cartao> findByBaralhoIdOrderById(Long baralhoId);

    /** A fila de estudo: o que venceu até hoje, os mais atrasados primeiro. */
    List<Cartao> findByBaralhoIdAndProximaRevisaoLessThanEqualOrderByProximaRevisaoAscIdAsc(Long baralhoId,
            LocalDate hoje);

    /**
     * Projeção: em vez de carregar entidades, a consulta devolve só os números que interessam,
     * já agrupados pelo banco. Os aliases (as total...) casam com os getters da interface.
     */
    interface Contagem {
        Long getBaralhoId();

        long getTotal();

        long getParaHoje();
    }

    @Query("""
            select c.baralho.id as baralhoId,
                   count(c) as total,
                   sum(case when c.proximaRevisao <= :hoje then 1 else 0 end) as paraHoje
            from Cartao c
            group by c.baralho.id
            """)
    List<Contagem> contarPorBaralho(LocalDate hoje);
}
