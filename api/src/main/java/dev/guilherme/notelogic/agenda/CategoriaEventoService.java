package dev.guilherme.notelogic.agenda;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.agenda.AgendaDtos.CategoriaRequest;
import dev.guilherme.notelogic.erro.ConflitoException;
import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;
import dev.guilherme.notelogic.usuario.UsuarioAtual;

@Service
public class CategoriaEventoService {

    private final CategoriaEventoRepository repository;
    private final UsuarioAtual usuario;

    public CategoriaEventoService(CategoriaEventoRepository repository, UsuarioAtual usuario) {
        this.repository = repository;
        this.usuario = usuario;
    }

    @Transactional(readOnly = true)
    public List<CategoriaEvento> listar() {
        return repository.findAllByUsuarioIdOrderByNomeAsc(usuario.id());
    }

    /** A de outro usuário dá o mesmo 404 de uma que não existe (ver MateriaService.buscar). */
    @Transactional(readOnly = true)
    public CategoriaEvento buscar(Long id) {
        return repository.findByIdAndUsuarioId(id, usuario.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
    }

    @Transactional
    public CategoriaEvento criar(CategoriaRequest dados) {
        if (repository.existsByUsuarioIdAndNomeIgnoreCase(usuario.id(), dados.nome())) {
            throw new ConflitoException("Já existe uma categoria chamada " + dados.nome());
        }
        return repository.save(new CategoriaEvento(usuario.id(), dados.nome(), dados.cor(), dados.destaqueOuPadrao()));
    }

    @Transactional
    public CategoriaEvento editar(Long id, CategoriaRequest dados) {
        CategoriaEvento categoria = buscar(id);
        if (repository.existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(usuario.id(), dados.nome(), id)) {
            throw new ConflitoException("Já existe uma categoria chamada " + dados.nome());
        }
        categoria.editar(dados.nome(), dados.cor(), dados.destaqueOuPadrao());
        return categoria;
    }

    /** Os eventos ficam: o banco (on delete set null) os deixa sem categoria. */
    @Transactional
    public void remover(Long id) {
        repository.delete(buscar(id));
    }
}
