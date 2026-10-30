package dev.guilherme.notelogic.agenda;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * As categorias com que toda conta nova começa. A pessoa pode editar e apagar.
 * As contas que já existiam ganharam as mesmas na V8 — mudou aqui, muda lá também.
 */
@Component
public class CategoriasPadrao {

    private record Padrao(String nome, String cor, boolean destaque) {
    }

    private static final List<Padrao> PADROES = List.of(
            new Padrao("Prova", "#E5484D", true),
            new Padrao("Aula", "#3B82F6", false),
            new Padrao("Treino", "#22A06B", false),
            new Padrao("Compromisso", "#F59E0B", false));

    private final CategoriaEventoRepository repository;

    public CategoriasPadrao(CategoriaEventoRepository repository) {
        this.repository = repository;
    }

    /** Roda dentro da transação do cadastro. */
    public void criarPara(Long usuarioId) {
        repository.saveAll(PADROES.stream()
                .map(p -> new CategoriaEvento(usuarioId, p.nome(), p.cor(), p.destaque()))
                .toList());
    }
}
