package dev.guilherme.notelogic.flashcard;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;

@Service
public class CartaoService {

    private final CartaoRepository repository;
    private final RevisaoRepository revisoes;
    private final BaralhoService baralhos;
    private final Clock clock;

    public CartaoService(CartaoRepository repository, RevisaoRepository revisoes, BaralhoService baralhos,
            Clock clock) {
        this.repository = repository;
        this.revisoes = revisoes;
        this.baralhos = baralhos;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Cartao> listar(Long baralhoId) {
        baralhos.buscar(baralhoId); // 404 se o baralho não existir, em vez de uma lista vazia
        return repository.findByBaralhoIdOrderById(baralhoId);
    }

    @Transactional(readOnly = true)
    public List<Cartao> filaDeEstudo(Long baralhoId) {
        baralhos.buscar(baralhoId);
        return repository.findByBaralhoIdAndProximaRevisaoLessThanEqualOrderByProximaRevisaoAscIdAsc(baralhoId,
                hoje());
    }

    @Transactional(readOnly = true)
    public Cartao buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cartão", id));
    }

    @Transactional
    public Cartao criar(Long baralhoId, String frente, String verso) {
        Baralho baralho = baralhos.buscar(baralhoId);
        return repository.save(new Cartao(baralho, frente, verso, hoje()));
    }

    @Transactional
    public Cartao editar(Long id, String frente, String verso) {
        Cartao cartao = buscar(id);
        cartao.editar(frente, verso);
        return cartao;
    }

    @Transactional
    public void remover(Long id) {
        repository.delete(buscar(id));
    }

    /** Reagenda o cartão e guarda a resposta no histórico, tudo na mesma transação. */
    @Transactional
    public Cartao revisar(Long id, Resposta resposta) {
        Cartao cartao = buscar(id);
        cartao.responder(resposta, hoje());
        revisoes.save(new Revisao(cartao, resposta, cartao.estado().intervaloDias(), clock.instant()));
        return cartao;
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }
}
