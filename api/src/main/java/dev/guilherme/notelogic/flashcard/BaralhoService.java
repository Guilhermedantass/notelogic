package dev.guilherme.notelogic.flashcard;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;
import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.materia.MateriaService;
import dev.guilherme.notelogic.usuario.UsuarioAtual;

@Service
public class BaralhoService {

    /** Um baralho com os números que aparecem no card da tela. */
    public record Resumo(Baralho baralho, long totalCartoes, long paraRevisarHoje) {
    }

    private final BaralhoRepository repository;
    private final CartaoRepository cartoes;
    private final MateriaService materias;
    private final Clock clock;
    private final UsuarioAtual usuario;

    public BaralhoService(BaralhoRepository repository, CartaoRepository cartoes, MateriaService materias,
            Clock clock, UsuarioAtual usuario) {
        this.repository = repository;
        this.cartoes = cartoes;
        this.materias = materias;
        this.clock = clock;
        this.usuario = usuario;
    }

    /** Duas consultas no total, não importa quantos baralhos existam. */
    @Transactional(readOnly = true)
    public List<Resumo> listar(Long materiaId) {
        Map<Long, CartaoRepository.Contagem> contagens = contagens();
        return repository.filtrar(usuario.id(), materiaId).stream()
                .map(b -> resumir(b, contagens.get(b.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Resumo detalhar(Long id) {
        Baralho baralho = buscar(id);
        return resumir(baralho, contagens().get(id));
    }

    @Transactional(readOnly = true)
    public Baralho buscar(Long id) {
        return repository.findByIdAndUsuarioId(id, usuario.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Baralho", id));
    }

    @Transactional
    public Baralho criar(String nome, String descricao, Long materiaId) {
        return repository.save(new Baralho(usuario.id(), nome, descricao, materiaDe(materiaId)));
    }

    @Transactional
    public Baralho editar(Long id, String nome, String descricao, Long materiaId) {
        Baralho baralho = buscar(id);
        baralho.editar(nome, descricao, materiaDe(materiaId));
        return baralho;
    }

    @Transactional
    public void remover(Long id) {
        repository.delete(buscar(id));
    }

    private Map<Long, CartaoRepository.Contagem> contagens() {
        return cartoes.contarPorBaralho(usuario.id(), LocalDate.now(clock)).stream()
                .collect(Collectors.toMap(CartaoRepository.Contagem::getBaralhoId, Function.identity()));
    }

    private static Resumo resumir(Baralho baralho, CartaoRepository.Contagem contagem) {
        return contagem == null
                ? new Resumo(baralho, 0, 0)
                : new Resumo(baralho, contagem.getTotal(), contagem.getParaHoje());
    }

    private Materia materiaDe(Long materiaId) {
        return materiaId != null ? materias.buscar(materiaId) : null;
    }
}
